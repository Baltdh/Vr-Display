# Instruções para agentes de IA e colaboradores

## Missão
Desenvolver o Vr-Display: aplicativo Android nativo para VR Box/Cardboard, inicialmente no Motorola Moto G52 (Android 13), com visualização SBS, passthrough de câmera, calibração, controles Bluetooth/OTG e evolução gradual para ARCore.

## Leitura obrigatória
Antes de qualquer mudança, leia README.md, este arquivo, docs/ARCHITECTURE.md, fontes relacionadas e workflows.

## Integridade do projeto
- O repositório anterior Baltdh/VrBox-Display foi excluído e seus arquivos-fonte não estão acessíveis pela integração atual.
- A restauração deve usar o histórico técnico e as decisões já documentadas, sem afirmar que os fontes foram copiados literalmente.
- Preserve o que já estiver implementado; não substitua funcionalidade por mock ou tela estática.
- Diferencie implementado, compilado no CI, testado em emulador e testado fisicamente.
- Nunca afirme que o Moto G52 foi testado sem confirmação física.

## Regras Android
- JDK 17, compileSdk 35, minSdk 26, AGP 8.7.3, Gradle 8.9.
- Kotlin, Jetpack Compose, Material 3; CameraX 1.4.1; ARCore 1.57.0.
- Solicitar permissões no contexto da ação do usuário.
- Respeitar lifecycle; liberar analyzer, use cases, Session e executors. No CameraX, proteger callbacks tardios após descarte, fechar sempre ImageProxy em finally e desvincular somente os use cases pertencentes ao painel.
- Não usar unbindAll() para encerrar apenas um painel.
- Evitar concorrência CameraX/ARCore; coordenar a posse da câmera.
- Fechar ImageProxy em finally.
- Não fazer processamento pesado no main thread.
- Não iniciar captura de tela sem consentimento.

## Limites de plataforma
- App Android comum não pode duplicar arbitrariamente toda a interface do sistema em SBS.
- MediaProjection exige autorização explícita e foreground service; FLAG_SECURE/DRM pode bloquear captura.
- Incorporar Activity de terceiros em painel 3D não é API universal; depende de versão, OEM e permissões.
- Um único frame de câmera duplicado nos olhos é mono, não estéreo real.
- Depth API estima profundidade, não substitui visão estéreo.
- ARCore precisa ser verificado em runtime e ter fallback.

## Qualidade
- Consultar documentação oficial para APIs novas.
- Fazer alterações pequenas e testáveis.
- Exibir erros úteis e manter fallback.
- Medir FPS, latência, CPU, memória e temperatura no Moto G52 antes de otimizar.
- Atualizar README e arquitetura em mudanças relevantes.
- Não publicar Release nem alterar visibilidade sem autorização.
- Não afirmar que APK existe sem artifact do commit correto.

## Prioridades
1. Restaurar fontes e obter build verde.
2. Corrigir lifecycle e persistir calibração/perfis.
3. Diagnóstico e mapeamento configurável de controles.
4. Medir CameraX SBS no Moto G52.
5. Planejar renderer OpenGL eficiente.
6. Implementar ARCore Session, tracking, renderização e Depth.
7. Explorar painéis Android somente dentro das APIs permitidas.
8. Planejar, sem antecipar a implementação da loja, a futura plataforma de jogos VR estilo Steam: runtime seguro, formato de pacote versionado, biblioteca local, criador visual no app e catálogo online. Seguir a Fase 8 do README; primeiro prototipar runtime/formato após estabilizar o renderer, depois editor e biblioteca local, e só então backend/loja.
9. Nunca executar pacotes de jogos com acesso irrestrito ao sistema. Projetar sandbox, permissões declaradas, assinatura e verificação de integridade desde o primeiro protótipo.

## Definição de concluído
Código, lifecycle/erros, build/testes pertinentes e documentação atualizados. Teste físico deve ser confirmado pelo usuário.
