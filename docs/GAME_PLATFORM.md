# Vr-Worlds — especificação inicial da plataforma de experiências VR

**Status:** proposta técnica inicial; não implementada no aplicativo.  
**Objetivo:** evoluir o Vr-Display para uma plataforma de experiências criadas pela comunidade, inspirada no modelo de criação e descoberta do Roblox, com distribuição semelhante a uma loja de jogos.

## 1. Conceito do produto

A plataforma não será apenas uma loja de jogos. O núcleo será um ecossistema de **mundos/experiências**:
- pessoas criam mundos e jogos;
- outras pessoas descobrem, instalam e jogam;
- criadores atualizam suas experiências;
- usuários podem ter identidade/avatar, amigos e participar de experiências compartilhadas;
- experiências poderão ser gratuitas e, no futuro, ter monetização sujeita a revisão legal, fiscal e de políticas.

O nome de trabalho para experiências é **Vr-Worlds**. O nome é provisório e não representa uma marca registrada.

## 2. Pilares

1. **Criar:** editor integrado ao Vr-Display, inicialmente visual e acessível a iniciantes.
2. **Jogar:** runtime comum para executar experiências compatíveis em modo tela única ou VR SBS.
3. **Descobrir:** catálogo, busca, categorias, páginas de experiências e recomendações.
4. **Compartilhar:** publicar versões, compartilhar links/códigos e permitir que a comunidade encontre criações.
5. **Social (etapa posterior):** perfis, avatares, amigos, salas e multiplayer.
6. **Segurança:** conteúdo moderado, permissões mínimas, pacotes assinados e execução isolada.

## 3. Escopo progressivo

### Etapa A — protótipo local offline
- Um projeto de mundo com cena, câmera, objetos 3D básicos, iluminação simples e spawn point.
- Editor de cena por toque e mouse/teclado/controle.
- Propriedades de posição, rotação, escala, cor/material e visibilidade.
- Componentes prontos: chão, parede, plataforma, cubo, esfera, luz, ponto de nascimento e texto.
- Interações simples por eventos/blocos: ao iniciar, ao tocar/colidir, mover, mostrar texto e trocar cena.
- Preview em tela única e VR SBS.
- Salvar/carregar projeto local e exportar pacote.

### Etapa B — runtime e pacote
- Runtime separado do editor.
- Manifesto versionado com ID, título, versão, runtime mínimo, orientação, modo VR, assets e permissões declaradas.
- Validação estrutural, limites de tamanho e integridade.
- Sem execução de código nativo arbitrário em pacotes baixados.
- APIs limitadas para input, cena, áudio, persistência local e recursos VR.
- Compatibilidade ARM64 e perfil de desempenho para smartphones modestos.

### Etapa C — biblioteca e descoberta
- Biblioteca local com instalar, abrir, atualizar e remover.
- Catálogo online com páginas de experiências, capturas, descrição, criador, versão e requisitos.
- Busca, categorias, tags, favoritos e avaliações com proteção contra abuso.
- Downloads retomáveis e atualização atômica, sem deixar instalação parcialmente corrompida.

### Etapa D — identidade e comunidade
- Conta opcional para sincronizar perfil e biblioteca.
- Nome público, avatar, perfil de criador e páginas de experiências.
- Amigos, presença e convites, sujeitos a controles de privacidade.
- Denúncia, bloqueio, filtros, classificação etária e ferramentas de moderação.

### Etapa E — multiplayer e economia de criadores
- Salas privadas e públicas; matchmaking e sincronização de estado.
- Servidor autoritativo para jogos que precisem de integridade competitiva.
- Ferramentas para publicar, versionar e reverter experiências.
- Monetização somente após política de conteúdo, segurança, suporte, pagamentos, impostos e regras aplicáveis estarem definidos.

## 4. Editor: primeira versão funcional

A primeira versão não tentará competir com Unity, Unreal ou Roblox Studio. Deve provar o ciclo completo com baixo custo computacional:
1. Criar projeto a partir de template.
2. Adicionar e selecionar objetos.
3. Mover, girar e redimensionar objetos.
4. Alterar propriedades básicas.
5. Adicionar um spawn point.
6. Testar a cena no runtime local.
7. Salvar e reabrir o projeto.

Primeiro template recomendado: **Sala de demonstração VR**, com piso, paredes, objetos interativos e ponto inicial. Segundo template: **Obby/Parkour**, por validar colisão, plataformas e checkpoints.

## 5. Arquitetura proposta

- **Creator UI:** editor de cenas e lógica visual.
- **Project Model:** modelo de dados declarativo, independente da UI.
- **World Runtime:** carrega projeto validado e executa a cena.
- **Renderer:** camada gráfica substituível, começando com protótipo 3D mínimo e evoluindo conforme medições.
- **Input Adapter:** abstrai toque, controle Bluetooth, teclado e mouse.
- **Package Manager:** valida, importa, exporta e instala pacotes.
- **Discovery/Store Client:** catálogo remoto, biblioteca e atualizações.
- **Platform Backend:** contas, distribuição, social, multiplayer e moderação em fases posteriores.

Editor, runtime e loja devem ser módulos separados. A experiência carregada não recebe privilégios do aplicativo hospedeiro.

## 6. Decisões técnicas em aberto

Antes de selecionar engine ou backend, criar provas de conceito e comparar:
- renderer 3D leve próprio versus integração de engine existente;
- formato declarativo de cena JSON versus binário;
- scripting visual por blocos/eventos versus linguagem embutida sandboxed;
- persistência local e migração de versões;
- renderização em dispositivos ARM64 de entrada;
- limites de memória, tamanho de mundo, draw calls e taxa de quadros;
- requisitos de rede e custo de servidores para multiplayer.

Nenhuma engine ou serviço de backend deve ser escolhido apenas por popularidade. Avaliar licença, manutenção, tamanho, compatibilidade e custo.

## 7. Segurança e moderação

- Pacotes publicados devem ter hash e assinatura verificáveis.
- Validar manifesto, tipos de arquivo, caminhos e tamanho antes de extrair.
- Bloquear path traversal, arquivos executáveis inesperados e permissões não declaradas.
- Isolar armazenamento de cada experiência.
- Limitar CPU, memória, entidades, áudio e tempo de execução.
- Não permitir acesso direto a câmera, microfone, contatos, localização ou arquivos do usuário sem uma API explícita e consentimento contextual.
- Multiplayer exige proteção contra spam, abuso, trapaça e exposição de dados pessoais.
- Conteúdo público requer denúncia, revisão, bloqueio e classificação etária.

## 8. Critérios do primeiro marco

O primeiro marco de Vr-Worlds será considerado demonstrado quando:
- o usuário criar um projeto local;
- adicionar e transformar objetos básicos;
- salvar e reabrir o projeto;
- executar a cena em um runtime separado do editor;
- alternar preview de tela única e SBS;
- exportar e importar um pacote validado.

Não inclui ainda contas, loja online, multiplayer, avatar social ou monetização.

## 9. Relação com o roadmap principal

A prioridade imediata continua sendo integridade do projeto, estabilidade CameraX/SBS, calibração, controles e desempenho. A prova de conceito de Vr-Worlds começa como módulo isolado e não deve comprometer o visualizador VR existente. Consultar a Fase 8 do README para a visão de longo prazo.
