# Arquitetura técnica

## Plataforma
Android nativo, Kotlin, Jetpack Compose/Material 3; JDK 17; compileSdk 35; minSdk 26; AGP 8.7.3; Gradle 8.9; CameraX 1.4.1; ARCore 1.57.0.

## Componentes
- UI: modos, calibração, permissões, status e diagnóstico.
- CameraX passthrough: aquisição de frames, orientação, throttling e composição SBS.
- Input: KeyEvents, controle Bluetooth, teclado e mouse OTG.
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

Duplicar o mesmo frame nos dois olhos é passthrough mono; não cria paralaxe nem profundidade binocular.

## ARCore
O preflight de disponibilidade/instalação não constitui uma Session ativa. A Session deve respeitar lifecycle, coordenar exclusividade da câmera com CameraX, tratar instalação/permissões/indisponibilidade, encerrar no pause/stop, verificar suporte a Depth e manter fallback.

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

A visão é um ecossistema de criação e descoberta inspirado no Roblox: editor visual, runtime comum, biblioteca, catálogo, e em fases posteriores identidade, social e multiplayer. A especificação inicial e decisões em aberto estão em [`GAME_PLATFORM.md`](GAME_PLATFORM.md). O manifesto de exemplo está em [`examples/vrworld.manifest.json`](examples/vrworld.manifest.json).

O primeiro protótipo será offline: criar uma sala, adicionar/transformar objetos, salvar/reabrir e executar em runtime separado, com preview flat/SBS. Não iniciar backend, pagamentos ou multiplayer neste marco. O runtime deve carregar apenas dados declarativos validados, com limites de recursos e sem executar código nativo arbitrário.
