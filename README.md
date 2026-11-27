# Vr-Display

Aplicativo Android nativo para transformar um smartphone em uma central VR/AR para headsets simples VR Box / Google Cardboard, sem root.

**Repositório oficial atual:** `Baltdh/Vr-Display`  
**Branch de desenvolvimento atual:** `feature/restore-project`  
**Aparelho inicial de validação:** Motorola Moto G52, Android 13.

## 1. Visão do produto

O aplicativo deve permitir que o celular seja usado normalmente como fonte de imagem e controle, enquanto o headset VR Box funciona como visualizador óptico. O app precisa oferecer:

- modo de tela única e modo VR lado a lado (SBS);
- passthrough usando câmera frontal ou traseira;
- calibração da imagem para ajustar o encaixe nas lentes;
- suporte a controle Bluetooth, teclado e mouse USB OTG;
- visualização de conteúdo Android em painéis, quando as APIs públicas permitirem;
- evolução para experiências AR com ARCore, tracking espacial e Depth API;
- interface simples, com permissões contextuais, indicação de câmera ativa e saída rápida.

A experiência deve funcionar em aparelhos Android comuns, com recursos opcionais ativados conforme a compatibilidade do dispositivo.

## 2. Histórico e restauração

O repositório anterior `Baltdh/VrBox-Display` foi excluído. Seus arquivos-fonte não estão disponíveis para recuperação pela integração atual. A implementação presente neste repositório é uma **reconstrução baseada no histórico técnico, decisões e requisitos registrados durante o desenvolvimento**, não uma cópia literal dos commits ou arquivos antigos.

O agente que assumir o projeto deve preservar essa distinção e não inventar testes ou funcionalidades que não estejam comprovados.

## 3. Estado atual do código

### Implementado na base restaurada

- Projeto Android com Kotlin, Jetpack Compose e Material 3.
- Tela inicial com alternância entre SBS e tela única.
- Seleção inicial entre câmera frontal e traseira.
- Controles visuais iniciais de separação, zoom, deslocamento horizontal/vertical e rotação.
- Pipeline CameraX com ImageAnalysis, estratégia `KEEP_ONLY_LATEST`, resolução-alvo 640×480, correção de rotação e limitação aproximada da atualização a 15 fps.
- Composição do mesmo frame nas duas regiões SBS.
- Diagnóstico inicial de eventos de tecla para controle Bluetooth e teclado.
- Painel de verificação de disponibilidade e solicitação de instalação do ARCore.
- Workflow GitHub Actions para compilar APK debug.
- Documentação de arquitetura e instruções para agentes.

### Parcial, provisório ou ainda não validado

- Os ajustes ópticos são persistidos localmente com Preferences DataStore; perfis nomeados de headset ainda não foram implementados.
- A renderização CameraX usa Bitmap/Canvas como primeira etapa; desempenho real ainda precisa ser medido no Moto G52.
- O ciclo de vida da câmera precisa ser exercitado em abertura/fechamento repetidos, troca de câmera, rotação, pause/resume e perda de permissão.
- O painel ARCore é somente preflight. Ainda não existe Session ARCore ativa, tracking, renderização 3D nem Depth API.
- Diagnóstico de KeyEvents não é ainda um sistema completo de mapeamento configurável.
- Teclado e mouse OTG dependem dos eventos Android padrão; ainda precisam de testes físicos.
- Não há ainda um sistema de painéis capaz de hospedar livremente qualquer aplicativo Android.
- Não há validação física confirmada nem APK de distribuição aprovado.

**Atenção:** build verde no CI prova que o código compila no ambiente do GitHub; não prova funcionamento no Moto G52.

## 4. Plano de desenvolvimento por etapas

O plano abaixo define a ordem de trabalho. O agente deve concluir e validar cada etapa antes de avançar para a seguinte, salvo correções pequenas que sejam pré-requisito.

### Fase 0 — Integridade da base e build reproduzível

Objetivo: garantir que qualquer colaborador consiga clonar, abrir e compilar o projeto.

Tarefas:
1. Conferir estrutura Gradle, Manifest, namespace, applicationId e versões.
2. Executar `gradle :app:assembleDebug` no CI.
3. Corrigir erros de compilação e avisos relevantes.
4. Confirmar que o artifact `vr-display-debug-apk` foi gerado para o commit correto.
5. Registrar no README o resultado e o SHA testado.

Critério de conclusão: build verde e APK debug disponível como artifact do workflow. Não confundir isso com teste físico.

### Fase 1 — Câmera e visualização SBS estáveis

Objetivo: tornar o passthrough previsível e seguro.

Tarefas:
1. Revisar CameraX e o ciclo de vida de `ProcessCameraProvider`, `ImageAnalysis` e executor.
2. Garantir fechamento de todo `ImageProxy` em `finally`.
3. Encerrar somente os use cases pertencentes ao painel; evitar `unbindAll()` indiscriminado.
4. Tratar falha de câmera, permissão negada, ausência de câmera frontal e troca de lente.
5. Testar abrir/fechar repetidamente, pause/resume, troca frontal/traseira e retorno de configurações.
6. Corrigir enquadramento, proporção, rotação e recorte sem distorção inesperada.
7. Verificar a separação entre as duas imagens e manter o modo tela única como fallback.

Critério de conclusão: fluxo sem crash ou vazamento aparente nos cenários de lifecycle, com comportamento documentado em emulador e, posteriormente, no aparelho físico.

### Fase 2 — Calibração óptica e perfis

Objetivo: adaptar a imagem a diferentes headsets, lentes e usuários.

Controles previstos:
- distância/separação visual entre as duas regiões;
- escala/zoom;
- deslocamento horizontal e vertical;
- rotação;
- recorte e proporção;
- correção de distorção óptica, caso seja tecnicamente viável;
- brilho e controles de visualização necessários.

Tarefas:
1. Separar estado de interface da configuração de renderização.
2. Persistir preferências com DataStore.
3. Criar perfis nomeados de headset e permitir restaurar padrão.
4. Definir limites e passos úteis para cada ajuste.
5. Adicionar uma tela de calibração com padrões geométricos e texto para alinhamento.
6. Documentar quais parâmetros são aproximações e quais representam medidas físicas.

Critério de conclusão: configurações sobrevivem ao fechamento do app e os perfis podem ser criados, selecionados, editados e redefinidos.

### Fase 3 — Controles Bluetooth, teclado e mouse OTG

Objetivo: permitir operação sem tocar no celular dentro do headset.

Tarefas:
1. Mapear KeyEvents: keyCode, scanCode, ação, repetição e dispositivo de origem.
2. Criar tela de diagnóstico com leitura em tempo real.
3. Implementar mapeamento configurável para ações do app.
4. Suportar navegação por D-pad, confirmação, voltar, gatilhos e botões disponíveis.
5. Detectar teclado e mouse por eventos padrão do Android.
6. Oferecer sensibilidade, zonas mortas e opção de redefinir mapeamentos.
7. Evitar interceptar teclas necessárias ao sistema e manter acessibilidade.

Critério de conclusão: controle Bluetooth, teclado e mouse OTG testados no aparelho-alvo, com mapa de botões documentado. Não presumir que todos os controles usam os mesmos códigos.

### Fase 4 — Desempenho e renderer gráfico

Objetivo: reduzir latência, uso de CPU e aquecimento.

Tarefas:
1. Medir resolução, FPS real, latência aproximada, CPU, memória e temperatura.
2. Registrar medições com a implementação Bitmap atual como baseline.
3. Investigar SurfaceTexture, OpenGL ES ou pipeline GPU equivalente.
4. Implementar renderer separado da UI Compose, se os dados justificarem.
5. Evitar conversões Bitmap desnecessárias e cópias de frame.
6. Manter fallback funcional para aparelhos sem suporte ao caminho acelerado.
7. Comparar qualidade, estabilidade e consumo antes/depois.

Critério de conclusão: medições reproduzíveis e melhoria comprovada sem regressão de qualidade ou estabilidade no aparelho-alvo.

### Fase 5 — ARCore: Session, tracking e Depth

Objetivo: evoluir do passthrough simples para experiência AR espacial.

Tarefas:
1. Verificar compatibilidade real com ARCore em runtime.
2. Implementar fluxo de instalação/atualização e tratamento de indisponibilidade.
3. Criar gerenciador explícito de posse da câmera para alternar CameraX e ARCore sem concorrência.
4. Implementar Session ligada ao lifecycle da Activity.
5. Adicionar tracking de movimento e pose da câmera.
6. Implementar renderer para objetos e planos virtuais.
7. Verificar suporte a Depth antes de habilitar a API e oferecer fallback.
8. Tratar perda de tracking, pausa, retomada, mudança de configuração e encerramento.
9. Testar no Moto G52; não presumir que suporte listado significa que todos os recursos estão ativos em qualquer versão.

Critério de conclusão: sessão real funcionando, tracking demonstrado e Depth habilitado somente quando suportado. O painel de preflight isoladamente não satisfaz esta fase.

### Fase 6 — Painéis de conteúdo Android e experiência espacial

Objetivo: exibir conteúdo e ferramentas Android em uma interface espacial.

Investigar e implementar apenas caminhos suportados:
1. Painéis próprios do Vr-Display para navegador, mídia local, configurações e ferramentas internas.
2. MediaProjection para captura autorizada pelo usuário, com foreground service e indicador visível.
3. Controles de início/parada e tratamento de apps que bloqueiam captura.
4. Integrações específicas com intents e APIs públicas quando apropriado.
5. Avaliar multi-window/freeform conforme versão Android e suporte do fabricante.
6. Organizar painéis em posições e tamanhos configuráveis quando tracking espacial estiver disponível.

Critério de conclusão: cada tipo de painel tem escopo e limitações documentados, consentimento respeitado e teste independente.

### Fase 7 — Experiência final, testes e distribuição

Objetivo: preparar uma versão estável para uso cotidiano.

Tarefas:
1. Revisar acessibilidade, textos, estados vazios, erros e permissões.
2. Testar instalação limpa, atualização, rotação, pause/resume e encerramento.
3. Testar os modos VR, passthrough, controles e AR nos aparelhos compatíveis.
4. Criar checklist de regressão.
5. Gerar APK debug para testes e, quando autorizado, APK release assinado.
6. Documentar instalação, requisitos, compatibilidade e limitações.
7. Não publicar release nem alterar visibilidade do repositório sem autorização explícita.

Critério de conclusão: checklist aprovado, problemas conhecidos registrados e artefato correspondente ao commit final disponível.

### Fase 8 — Plataforma de jogos VR: criação, biblioteca e loja própria

**Visão de longo prazo:** transformar o Vr-Display também em uma plataforma própria de criação e distribuição de jogos VR, com biblioteca e loja online. O usuário poderá descobrir, instalar, organizar e executar jogos compatíveis e, futuramente, criar experiências/jogos dentro do próprio aplicativo e compartilhar ou publicar suas criações na loja da plataforma.

Esta é uma expansão futura do produto. Não deve atrasar a estabilização do visualizador VR, câmera, controles e ARCore. A arquitetura deve, porém, evitar decisões que impeçam essa evolução.

#### 8.1 — Biblioteca e launcher de jogos
- Criar uma biblioteca local de jogos e experiências instaladas.
- Exibir capa, título, descrição, versão, tamanho, requisitos, compatibilidade e controles.
- Iniciar jogos instalados e permitir favoritos, categorias, pesquisa e histórico.
- Reconhecer formatos/pacotes de jogos definidos pelo projeto, sem executar arquivos arbitrários.
- Permitir importação local de pacotes confiáveis, com validação de integridade e permissões.

#### 8.2 — Loja online estilo Steam
- Criar catálogo remoto com páginas de jogos, imagens, vídeos, descrição, desenvolvedor, versão, requisitos e avaliações.
- Implementar contas/perfis de usuário, biblioteca vinculada à conta e sincronização, respeitando privacidade.
- Permitir jogos gratuitos inicialmente; compras, pagamentos e divisão de receita só depois de análise jurídica, fiscal, operacional e das políticas das lojas/plataformas.
- Oferecer download, instalação, atualização, pausa/retomada e remoção de jogos.
- Ter canais de lançamento (estável/beta), changelog, verificação de assinatura/hash e rollback quando tecnicamente possível.
- Criar ferramentas de denúncia, moderação, classificação etária e proteção contra conteúdo malicioso.
- Projetar backend e API com autenticação segura, autorização por usuário, rate limits, logs sem dados sensíveis e proteção contra abuso.

#### 8.3 — Criador de jogos dentro do Vr-Display
- Desenvolver um modo **Criar** dentro do aplicativo, começando por modelos prontos e experiências simples.
- Oferecer editor visual de cenas com objetos, textos, imagens, áudio, posicionamento, escala e interação por controle/toque.
- Permitir configurar câmera, movimento, colisões, gatilhos e lógica simples por blocos ou eventos, sem exigir programação para projetos básicos.
- Incluir pré-visualização imediata em tela única e VR SBS.
- Permitir salvar projetos, duplicar modelos, exportar pacotes e testar localmente.
- Evoluir gradualmente para scripts/API para desenvolvedores avançados, documentação e SDK.
- Separar editor, runtime de jogos e serviços da loja; um jogo não deve executar com privilégios do aplicativo principal.

#### 8.4 — Runtime e compatibilidade
- Definir um formato de pacote versionado para jogos, com manifesto, assets, versão mínima do runtime, resolução/modo de visualização, controles e permissões declaradas.
- Criar runtime isolado com APIs limitadas para gráficos, áudio, input, armazenamento do jogo e recursos VR/AR.
- Avaliar engine própria leve e/ou integração com engine existente; tomar a decisão por protótipos, tamanho do APK, desempenho, licença e compatibilidade ARM64.
- Não permitir código nativo arbitrário ou acesso irrestrito ao sistema em jogos baixados.
- Aplicar limites de memória, tempo de execução, acesso a arquivos e permissões; interromper jogos que travem ou excedam limites.
- Suportar primeiro jogos 3D simples e experiências VR Cardboard/SBS; expandir para AR e dispositivos mais potentes conforme capacidade.

#### 8.5 — Publicação e comunidade
- Criar fluxo para validar, testar e enviar jogos à loja.
- Exigir assinatura dos pacotes e verificação de integridade no download.
- Ter página do desenvolvedor, versões, notas de atualização, avaliações e política de conteúdo.
- Planejar revisão/moderação antes da publicação pública.
- Fornecer documentação, templates e SDK para criadores externos.

**Critérios para considerar esta fase concluída:** biblioteca local funcional; formato de pacote e runtime documentados; protótipo de criação que salva e executa um projeto simples; catálogo remoto com download/atualização segura; e processo de publicação/moderação definido. Loja com pagamentos e distribuição pública é uma etapa posterior e não deve ser simulada com dados fictícios.

**Dependências e ordem recomendada:** concluir as fases 0–4 primeiro. Em paralelo, elaborar protótipo técnico do runtime e do formato de pacote após a fase 4. Construir o criador e a biblioteca local antes de implementar contas, backend e loja online. Só depois abrir publicação para terceiros.

## 5. Regras técnicas e limites de plataforma

- Não exigir root.
- A câmera só inicia após ação explícita e permissão concedida.
- Exibir indicação clara de câmera ativa e opção de fechar imediatamente.
- Não coletar nem transmitir imagens sem uma função explicitamente autorizada.
- Uma câmera mono duplicada nos dois olhos continua sendo imagem mono; não cria paralaxe nem estereoscopia real.
- ARCore Depth estima profundidade; não equivale a duas câmeras estéreo.
- CameraX e ARCore podem disputar a câmera e precisam de coordenação explícita.
- MediaProjection exige consentimento do usuário e foreground service; conteúdo protegido por DRM ou `FLAG_SECURE` pode impedir captura.
- Um app Android comum não pode incorporar arbitrariamente qualquer Activity de terceiros em uma janela 3D.
- Não prometer suporte universal a painéis externos; documentar dependências de versão Android, fabricante e permissões.

## 6. Tecnologias e ambiente

- Kotlin
- Jetpack Compose e Material 3
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- JDK 17
- compileSdk 35
- minSdk 26
- CameraX 1.4.1
- ARCore 1.57.0
- Preferences DataStore 1.1.1

Build local:
```bash
gradle :app:assembleDebug
```

APK debug, após build bem-sucedido:
`app/build/outputs/apk/debug/app-debug.apk`

O workflow do GitHub Actions executa o build e publica o artifact `vr-display-debug-apk` por 7 dias.

## 7. Instruções para o próximo agente de IA

O agente deve atuar como colaborador técnico contínuo, não como gerador de uma implementação descartável.

1. Antes de editar, leia este README, `AGENTS.md` e `docs/ARCHITECTURE.md`.
2. Verifique a branch atual, HEAD, status do CI e arquivos existentes.
3. Trabalhe preferencialmente em branch de feature; não faça mudanças diretas em `main` sem necessidade e autorização.
4. Não recrie arquivos que já existem sem antes inspecioná-los.
5. Preserve funcionalidades e decisões confirmadas; não substitua código por placeholders.
6. Divida o trabalho em commits pequenos e com mensagens claras.
7. Antes de usar APIs Android/ARCore, confira a documentação oficial e a compatibilidade com as versões do projeto.
8. Execute build e testes pertinentes depois de cada conjunto de mudanças.
9. Analise logs reais do CI; não deduza sucesso apenas porque o workflow iniciou.
10. Atualize este README, `AGENTS.md` ou `docs/ARCHITECTURE.md` sempre que o estado, a arquitetura ou o plano mudar.
11. Relate claramente o que foi implementado, o que compilou, o que foi testado em emulador e o que foi testado fisicamente.
12. Não declarar o Moto G52 validado sem teste real confirmado.
13. Não publicar Release, trocar a visibilidade do repositório ou remover funcionalidades sem autorização do proprietário.

### Formato recomendado de relatório por etapa

- **Objetivo:** qual problema foi tratado.
- **Arquivos:** quais foram criados ou alterados.
- **Implementação:** o que mudou tecnicamente.
- **Validação:** build, testes e logs consultados.
- **Resultado:** concluído, parcial ou bloqueado.
- **Riscos/limitações:** o que ainda precisa ser verificado.
- **Próxima tarefa:** menor próximo incremento útil.

## 8. Documentação complementar

- [Guia de continuidade para agentes](AGENTS.md)
- [Arquitetura técnica](docs/ARCHITECTURE.md)
- [Vr-Worlds — especificação da plataforma de criação e jogos](docs/GAME_PLATFORM.md)
- [Manifesto de exemplo de uma experiência](docs/examples/vrworld.manifest.json)
- [Código-fonte](app/src/main/java/com/baltdh/vrdisplay/)
- [GitHub Actions](.github/workflows/android.yml)

### Registro da validação atual — 2026-11-27

- Foi adicionada persistência local dos cinco parâmetros de calibração (separação visual, zoom, deslocamentos horizontal/vertical e rotação), com gravação debounced para evitar escrita a cada movimento do slider. O commit `2fc06c7faf38f9aa355e649658c6ceb8e33d8750` compilou com sucesso e gerou o artifact `vr-display-debug-apk` (ID 15182439050; SHA-256 `bede9666158f16514d123ed7eb90e0664e1a070d2c247b08edda5f39dd45c52b`).

- O commit `aa14e2e5abe4e9a89f476a3e6aa07d00afee3328` concluiu o GitHub Actions com sucesso e gerou `vr-display-debug-apk` (artifact 15181594625; SHA-256 `026ae60b5c7b79e37160ac497c99c1b20301f35ef89baf064115ed69cf21b6a2`). Esse build valida somente aquele commit.
- Após a confirmação do build, `CameraPreview.kt` recebeu uma revisão do lifecycle: descarte sinalizado atomicamente, descarte do analyzer, desvinculação apenas do use case do painel, atualização de UI protegida contra callbacks tardios e throttling com relógio monotônico. A validação CI dessa revisão ainda está pendente.


- Um build anterior da base restaurada concluiu com sucesso no SHA `8a732e4a953e8f8ad3b53ba72f9ec083df4c51bb` e gerou o artifact `vr-display-debug-apk`.
- Builds posteriores identificaram erro de compilação em `CameraPreview.kt`: import incorreto de `toBitmap`. A API é um método de instância de `ImageProxy` no CameraX 1.3+; o import foi removido no commit `b5295e05682ea0f5e85ca13a32552208a25ba033`.
- **A correção ainda aguarda confirmação de um workflow concluído no SHA corrigido.** Não considerar o APK anterior como validação da versão atual.
- Nenhum teste físico no Moto G52 foi confirmado.

## 9. Estado de validação

A validação deve sempre ser atualizada com evidências do commit correspondente:
- **Código presente:** verificar os arquivos da branch.
- **Build:** verificar execução concluída com sucesso no SHA exato.
- **APK:** verificar artifact anexado ao workflow desse SHA.
- **Teste físico:** registrar aparelho, versão Android, cenário e resultado observado.

Não apresentar nenhum desses níveis como equivalente aos demais.
