# Vr-Display

Aplicativo Android para transformar um smartphone comum em uma central de visualização VR/AR para headsets simples do tipo VR Box / Google Cardboard.

## Dispositivo inicial de validação
- Motorola Moto G52
- Android 13
- Headset VR Box com lente dupla
- Controle Bluetooth branco
- Mouse e teclado via USB OTG (incluindo receptores sem fio)

## Objetivo
Construir uma aplicação Android nativa, sem root, que inicie como qualquer aplicativo Android e ofereça:
- visualização em tela única e modo VR lado a lado (SBS);
- câmera frontal ou traseira como passthrough, duplicada para as duas lentes;
- ajustes ópticos persistentes: separação/IPD visual, zoom, deslocamentos horizontal/vertical e rotação;
- diagnóstico e mapeamento de controle Bluetooth por eventos de tecla;
- compatibilidade com mouse e teclado USB OTG;
- evolução para ARCore, tracking espacial, Depth API e painéis espaciais;
- interface para abrir conteúdo Android em painéis quando as APIs e as políticas do sistema permitirem.

## Estado da implementação restaurada
O repositório anterior `Baltdh/VrBox-Display` foi excluído e não está disponível para recuperar seus arquivos-fonte pela integração atual. Esta nova base será reconstruída a partir do histórico técnico e das decisões documentadas durante o desenvolvimento anterior. Os recursos são separados entre implementação presente nesta base e etapas planejadas. Não presumir que o código antigo foi copiado byte a byte.

### Nesta restauração
- documentação de arquitetura e continuidade;
- estrutura Android/Kotlin/Jetpack Compose e pipeline CameraX SBS a ser restaurado;
- calibração e diagnóstico de controles a serem reintroduzidos;
- preflight ARCore a ser reintroduzido.

### Ainda exige implementação/validação
- persistência dos ajustes e perfis;
- seleção completa de câmera e tratamento de lifecycle em todos os cenários;
- medição de desempenho no Moto G52 e otimização GPU/OpenGL;
- criação de sessão ARCore, tracking e renderização 3D;
- Depth API;
- integração de apps de terceiros em painéis (sujeita às limitações do Android);
- mapeamento configurável de todos os botões/controles;
- testes físicos e APK de distribuição.

## Arquitetura e documentação
- [Arquitetura técnica](docs/ARCHITECTURE.md)
- [Guia de continuidade para agentes](AGENTS.md)

## Requisitos
- Android Studio recente, JDK 17, Android SDK Platform 35
- Gradle 8.9 no CI
- Dispositivo/emulador Android API 26 ou superior

## Build
No Android Studio: abrir a pasta do projeto e sincronizar Gradle.
Pela linha de comando, com Gradle instalado:
```bash
gradle :app:assembleDebug
```
O APK debug, quando a compilação terminar com sucesso, fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Segurança e privacidade
A câmera só deve ser iniciada após ação explícita do usuário e concessão da permissão. A interface deve indicar quando a câmera estiver ativa e oferecer saída imediata. Não coletar nem transmitir imagens.

## Limites técnicos
- Não exigir root.
- Não iniciar captura de tela global silenciosamente.
- Não prometer que qualquer aplicativo Android poderá ser incorporado em uma janela VR: MediaProjection exige consentimento e alguns apps bloqueiam captura; Activity embedding e janelas livres dependem do suporte do Android/OEM.
- Uma única câmera duplicada nas duas lentes produz imagem binocular idêntica (mono duplicado), não profundidade estereoscópica real.
- ARCore Depth estima profundidade; não equivale a duas câmeras estéreo.
- CameraX e ARCore podem disputar a câmera. Os modos devem ser mutuamente exclusivos ou coordenados por um gerenciador de câmera.
- Não declarar teste físico concluído sem evidência no aparelho.

## Fluxo de trabalho
1. Ler README, `AGENTS.md` e `docs/ARCHITECTURE.md` antes de alterar código.
2. Inspecionar branch, fontes e workflows.
3. Fazer mudanças pequenas, executar build/testes e atualizar documentação.
4. Só indicar APK depois de confirmar build verde e artifact do commit correspondente.

## CI
O workflow Android deverá executar `assembleDebug` em push e pull request. Build verde comprova compilação no CI, não funcionamento físico no Moto G52.
