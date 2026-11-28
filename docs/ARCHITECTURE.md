# Arquitetura técnica

## Plataforma
Android nativo, Kotlin, Jetpack Compose/Material 3; JDK 17; compileSdk 35; minSdk 26; AGP 8.7.3; Gradle 8.9; CameraX 1.4.1; ARCore 1.57.0.

## Componentes
- UI: modos, calibração, permissões, status, visualizador VR imersivo e diagnóstico.
- CameraX passthrough: aquisição de frames, orientação, throttling e composição SBS.
- Input: KeyEvents e GenericMotionEvents, identificação de dispositivos, controle Bluetooth, teclado, mouse/trackpad OTG e joystick; mapeador persistente de seis ações por KeyEvent.
- ARCore: disponibilidade/instalação, Session, tracking, Depth e renderer.
- Settings: preferências e perfis por headset/dispositivo.

## Fluxo CameraX esperado
1. Usuário solicita abrir a câmera.
2. App solicita CAMERA se necessário.
3. ImageAnalysis usa KEEP_ONLY_LATEST.
4. Aplicar imageInfo.rotationDegrees.
5. Limitar taxa de atualização e evitar trabalho pesado no main thread.
6. Renderizar o frame nas duas regiões (SBS) ou em tela única.
7. Fechar ImageProxy em finally e encerrar analyzer/executor no descarte.

Duplicar o mesmo frame nos dois olhos é passthrough mono; não cria paralaxe nem profundidade binocular. O modo imersivo oculta as barras do sistema via WindowInsetsControllerCompat e mantém um comando explícito de saída.

## ARCore
O painel de preflight consulta compatibilidade com `checkAvailabilityAsync()` e solicita instalação/atualização com `requestInstall(activity, true)`. Quando o retorno é `INSTALL_REQUESTED`, a Activity pode pausar durante o fluxo do Google Play Services para RA; no próximo `ON_RESUME`, o painel repete `requestInstall(activity, false)` para confirmar a conclusão ou exibir a falha. A confirmação de instalação não inicia uma Session. A implementação futura da Session deverá respeitar lifecycle, coordenar exclusividade da câmera com CameraX, tratar permissões/indisponibilidade, encerrar no pause/stop, verificar suporte a Depth e manter fallback. Referência: [ativar AR em um app Android](https://developers.google.com/ar/develop/java/enable-arcore).

## Captura e painéis
MediaProjection exige autorização explícita e possui restrições. DRM/FLAG_SECURE pode impedir captura. Não existe mecanismo público universal para incorporar qualquer Activity externa em uma cena 3D. Implementar somente integrações permitidas.

## Desempenho
Bitmap é uma etapa inicial, não a arquitetura final. Medir no Moto G52 resolução, FPS, latência, CPU, memória e temperatura antes de migrar para OpenGL/SurfaceTexture ou pipeline GPU.


## Plataforma futura de jogos VR (roadmap)

O produto poderá evoluir para uma plataforma de jogos inspirada na Steam, com biblioteca/launcher local, loja online, runtime de jogos e ferramenta de criação integrada ao app. Esta camada é futura e não deve comprometer as prioridades do visualizador VR/AR.

Componentes arquiteturais previstos:
- **Game Library:** catálogo local, metadados, favoritos, versões e estado de instalação.
- **Game Package:** formato versionado com manifesto, assets, requisitos de runtime, controles e permissões declaradas.
- **Game Runtime:** execução isolada com APIs limitadas de gráficos, áudio, input, armazenamento e recursos VR/AR.
- **Creator Studio:** editor visual de cenas, objetos, interações e lógica simples; preview em tela única/SBS; salvar, testar e exportar projetos.
- **Store Client:** catálogo remoto, contas, download/atualização, canais beta/estável, avaliações e páginas de desenvolvedor.
- **Backend:** autenticação/autorização, distribuição de pacotes, moderação, métricas minimizadas e proteção contra abuso.

Segurança obrigatória: pacotes assinados, hash verificado, permissões explícitas, sandbox, limites de recursos e ausência de execução nativa arbitrária por padrão. Compras e pagamentos exigem análise posterior das regras legais, fiscais e das políticas de distribuição.

Ordem recomendada: estabilizar fases 0–4; prototipar formato de pacote e runtime; desenvolver criador e biblioteca local; só depois desenvolver contas, backend, loja online, publicação e moderação. Consultar a Fase 8 do README para o escopo detalhado.


## Vr-Worlds — plataforma de experiências criadas pela comunidade

A visão é um ecossistema próprio de criação e descoberta de experiências VR: editor visual, runtime comum, biblioteca, catálogo e, em fases posteriores, identidade, recursos sociais e multiplayer. A especificação inicial e decisões em aberto estão em [`GAME_PLATFORM.md`](GAME_PLATFORM.md). O manifesto de exemplo está em [`examples/vrworld.manifest.json`](examples/vrworld.manifest.json).

O primeiro protótipo será offline: criar uma sala, adicionar/transformar objetos, salvar/reabrir e executar em runtime separado, com preview flat/SBS. Não iniciar backend, pagamentos ou multiplayer neste marco. O runtime deve carregar apenas dados declarativos validados, com limites de recursos e sem executar código nativo arbitrário.


## Revisão do lifecycle CameraX — 2026-11-27

A implementação atual do painel usa `AtomicBoolean` para sinalizar descarte entre a thread principal e o executor do analyzer. Atualizações de frame/erro são encaminhadas ao executor principal e ignoradas após descarte. No encerramento, o analyzer é limpo, somente o `ImageAnalysis` do painel é desvinculado e o executor é encerrado. O throttling usa `SystemClock.elapsedRealtime()` para não depender de alterações no relógio civil. A revisão foi compilada no commit `2e900adda5a85250e49dc6f2aab05a279b9055fb`; o CI aprovou testes unitários e `assembleDebug`. Isso não comprova ausência de vazamentos nem estabilidade no Moto G52, que ainda exige teste físico.


## Persistência de calibração

`CalibrationSettings` é o modelo único dos cinco ajustes ópticos. `CalibrationProfileState` organiza até 20 perfis independentes, com seleção do perfil ativo e criação por nome. `CalibrationSettingsStore` serializa perfis em JSON no Preferences DataStore privado do app e migra automaticamente os cinco valores legados para o perfil padrão. A UI carrega o estado salvo uma vez e grava alterações após uma pausa curta (debounce de 350 ms), evitando operações de escrita em cada evento do slider. Os valores são normalizados dentro dos limites aceitos antes de persistir. Sincronização entre aparelhos não faz parte desta implementação.


## Testes automatizados

`CalibrationSettingsTest` verifica os valores padrão, limites aceitos e normalização da rotação. `CalibrationProfileTest` cobre criação, seleção, isolamento de configurações, nomes duplicados e limite de 20 perfis. `InputMappingSettingsTest` cobre defaults, remapeamento, troca sem colisões e teclas desconhecidas. `JoystickAxisMapperTest` cobre quatro direções, zona morta configurável, histerese de ativação/liberação do stick e HAT, não repetição, rearme e normalização de amostras não finitas. O workflow Android CI executa `:app:testDebugUnitTest` antes de `:app:assembleDebug`, de modo que regressões do modelo de calibração bloqueiem a geração do APK.


## Diagnóstico de entrada

A Activity intercepta `dispatchKeyEvent` para identificar teclas, scan code, repetição, dispositivo e source. `InputMappingSettings` mapeia seis ações (confirmar, voltar, cima, baixo, esquerda, direita); o usuário arma uma ação e a próxima tecla DOWN é atribuída; o KEY_UP correspondente também é consumido para não acionar navegação involuntária do Android. Repetições da mesma tecla não disparam a ação mais de uma vez por pressionamento. Ao tentar usar uma tecla já atribuída, as duas ações trocam de tecla para evitar colisões. `InputMappingStore` persiste o mapa em Preferences DataStore separado. Confirmar alterna SBS/tela única, voltar fecha o visualizador imersivo e as direções ajustam a calibração em passos de 0,03. `dispatchGenericMotionEvent` registra movimento, hover, scroll e botões de mouse/trackpad/joystick. Para `SOURCE_JOYSTICK` e `SOURCE_GAMEPAD`, `JoystickAxisMapper` converte AXIS_X/AXIS_Y e AXIS_HAT_X/AXIS_HAT_Y nas quatro ações direcionais. A zona morta padrão do stick é 0,45 e pode ser ajustada entre 0,10 e 0,90 na interface, com persistência no DataStore. O HAT tem limiar de ativação 0,5 e prioridade sobre o stick; o limiar de liberação é 0,35. Para o stick, o limiar de liberação corresponde a 75% da zona morta configurada. Essa histerese evita que pequenas oscilações próximas ao limite rearmem repetidamente a mesma direção. Valores de eixo não finitos são normalizados para zero e os demais são limitados ao intervalo [-1, 1]. Cada direção é emitida uma vez até que a entrada retorne abaixo do limiar de liberação ou mude de direção. Mouse OTG usa clique primário para confirmar, secundário para voltar e roda vertical para cima/baixo. Os botões são mapeamentos convencionais fixos; trackpad continua em diagnóstico.


## Instrumentação de desempenho CameraX

`CameraPerformanceMeter` recebe amostras no único executor do `ImageAnalysis` e publica snapshots em janelas de 1 segundo: FPS efetivo dos frames aceitos pelo throttling, dimensões do Bitmap corrigido e média do tempo gasto em `ImageProxy.toBitmap()` mais correção de rotação. A UI apresenta esses dados no preview e no modo imersivo. A medição de processamento não é latência ponta a ponta: não inclui exposição, filas internas, agendamento de UI ou apresentação física. `CameraPerformanceMeterTest` cobre cálculo, janelas subsequentes e amostras de duração não finitas.


### Precisão da métrica de FPS

O FPS da janela é calculado pelo número de intervalos entre frames ((N - 1) / tempo decorrido), não pelo número bruto de amostras dividido pelo tempo. Isso evita contar o próprio primeiro frame como um intervalo. A métrica continua sendo FPS de frames processados, não latência física de apresentação.
