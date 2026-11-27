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
