# API Interclasse ETEC

API REST para organizar o **interclasse de final de ano** da ETEC.
Projeto educacional: a base está pronta (estrutura MVC, banco SQLite e login),
e o restante do sistema é implementado pelos alunos a partir das atividades deste README.

---

## 1. Como rodar

**Requisitos:** JDK 17+, Maven 3.8+

```bash
# sobe o Jetty embutido na porta 8082
mvn jetty:run
```

A API fica em `http://localhost:8082/api`.

> O banco `interclasse.db` (SQLite) é criado automaticamente na primeira conexão,
> junto com as tabelas e os dados iniciais.

**Teste rápido:** `GET http://localhost:8082/api`

> O `interclasse.db` é criado na pasta do projeto. Para começar do zero, apague o arquivo
> e suba o servidor de novo.

### Testando com curl

```bash
# cadastra participante
curl -X POST http://localhost:8082/api/auth/registrar -H "Content-Type: application/json" -d '{"nome":"Ana Souza","email":"ana@etec.sp.gov.br","senha":"123456"}'

# cadastra organizador (precisa da chave mestra)
curl -X POST http://localhost:8082/api/auth/registrar-organizador -H "Content-Type: application/json" -d '{"nome":"Prof Carlos","email":"carlos@etec.sp.gov.br","senha":"123456","chaveMestra":"ETEC-INTERCLASSE-2026"}'

# login (use -c cookies.txt para manter a sessao)
curl -X POST http://localhost:8082/api/auth/login -H "Content-Type: application/json" -c cookies.txt -d '{"email":"carlos@etec.sp.gov.br","senha":"123456"}'

# cria um time
curl -X POST http://localhost:8082/api/admin/times -H "Content-Type: application/json" -b cookies.txt -d '{"nome":"Falcao da Silva","cor":"Azul","responsavel":"Carlos","contato":"11988887777"}'
```

**Postman/Insomnia:** importe a coleção ou use as URLs acima. Lembre-se de ativar
`Cookie Manager` / `withCredentials` para as rotas que exigem login.

---

## 2. Estrutura (MVC)

```
src/main/java/com/example/
├── controller/     -> Servlets (rotas HTTP). Recebem a requisição, chamam o DAO e devolvem JSON
│   ├── ApiController.java
│   ├── AuthController.java
│   ├── TimeController.java
│   ├── PartidaController.java
│   ├── TurmaController.java
│   ├── ModalidadeController.java
│   └── InscricaoController.java
├── dao/            -> Acesso ao banco (SQL). Um DAO por entidade
│   ├── TimeDao.java
│   └── UsuarioDao.java
├── model/          -> Entidades (POJOs). Representam as tabelas
│   ├── Time.java
│   └── Usuario.java
├── filter/         -> Filtros de acesso (autenticacao/autorizacao)
│   └── OrganizadorFilter.java
└── util/           -> Utilitarios (conexao, JSON, sessao)
    ├── Conexao.java
    ├── JsonUtil.java
    └── SessaoUtil.java
```

**Fluxo de uma requisição:**
```
Request -> Filter (permissao) -> Controller (valida, orquestra) -> DAO (SQL) -> Model
        <- Controller monta o JSON <- DAO devolve os dados <-
```

---

## 3. Sistema de Login

Simples, baseado em **sessão** (`HttpSession`). Sem token, sem JWT.

| Tipo | Como se cadastra |
|---|---|
| `PARTICIPANTE` | Cadastro livre em `/api/auth/registrar` |
| `ORGANIZADOR` | Cadastro em `/api/auth/registrar-organizador`, **exige chave mestra** |

A chave mestra padrão é `ETEC-INTERCLASSE-2026` (definida na tabela `chave_mestra`).
Para gerar outras chaves, insira novas linhas na tabela pelo `sqlite3` ou pelo
método `UsuarioDao.cadastrarChave(String)`.

**Sessão:** após o login, o `usuarioId` e o `tipo` ficam guardados na sessão.
O `OrganizadorFilter` bloqueia as rotas `/api/admin/*` para quem não for organizador.

---

## 4. Entidades do sistema

O que **já existe** na base (`Conexao.criarTabelas`):

| Entidade | Tabela | Campos |
|---|---|---|
| `Usuario` | `usuario` | `id`, `nome`, `email`, `senha` (hash), `tipo`, `criado_em` |
| `Time` | `time` | `id`, `nome`, `cor`, `responsavel`, `contato`, `criado_em` |

O que os alunos **precisam criar** (Atividade 1 do README):

| Entidade | Tabela | Campos |
|---|---|---|
| `Turma` | `turma` | `id`, `nome` (ex: "2º DS"), `curso`, `ano` |
| `Modalidade` | `modalidade` | `id`, `nome` (FUTSAL, VOLEI, QUEIMADA...), `ativo` (0/1) |
| `Inscricao` | `inscricao` | `id`, `time_id`, `modalidade_id`, `data_inscricao`, `status` |
| `Partida` | `partida` | `id`, `modalidade_id`, `time_a_id`, `time_b_id`, `placar_a`, `placar_b`, `data`, `hora`, `local`, `fase`, `status` |
| `Configuracao` | `configuracao` | `chave`, `valor` (pontos e regras do campeonato) |

### Status e valores permitidos

| Campo | Valores |
|---|---|
| `usuario.tipo` | `PARTICIPANTE`, `ORGANIZADOR` |
| `inscricao.status` | `PENDENTE`, `CONFIRMADA`, `NEGADA` |
| `partida.status` | `AGENDADA`, `EM_ANDAMENTO`, `FINALIZADA`, `CANCELADA` |
| `partida.fase` | `AVULSO` (no MVP), depois `GRUPOS`, `QUARTAS`, `SEMI`, `FINAL` |

---

## 5. Regras de negócio

### 5.1 Inscrição
- Um time participa de **no máximo 1 modalidade por vez** (1 time por modalidade).
- A inscrição nasce como `PENDENTE`; só o organizador confirma (`CONFIRMADA`) ou nega (`NEGADA`).
- Só times com inscrição `CONFIRMADA` podem entrar em partida.

### 5.2 Partida
- Só pode ser criada entre dois times **confirmados na mesma modalidade**.
- Os dois times devem ser **diferentes**.
- O **placar só pode ser alterado** enquanto a partida não estiver `FINALIZADA`.
- Ao finalizar, o resultado alimenta a classificação.

### 5.3 Pontuação (configurável)
Os valores ficam na tabela `configuracao` e podem ser alterados sem mexer no código:

| Chave | Padrão | Significado |
|---|---|---|
| `PONTOS_VITORIA` | `3` | Pontos do vencedor |
| `PONTOS_EMPATE` | `1` | Pontos de cada time empatado |
| `PONTOS_DERROTA` | `0` | Pontos do perdedor |
| `PERMITE_EMPATE` | `1` | `1` = empate vale, `0` = não há empate |

> **Empate configurável:** se `PERMITE_EMPATE = 0`, a partida que terminar empatada
> não pode ser finalizada (retornar erro). Se for `1`, cada time leva `PONTOS_EMPATE`.

### 5.4 Classificação
Calculada a partir das partidas `FINALIZADAS` de uma modalidade:
1. `3` pontos pro vencedor, `1` pra cada um no empate, `0` pro perdedor.
2. Total de pontos.
3. Desempate 1: número de vitórias.
4. Desempate 2: saldo de gols (`placar_a + placar_b` pró - contra).
5. Desempate 3: nome do time (ordem alfabética).

---

## 6. Endpoints

### Auth
| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/registrar` | livre | Cadastra participante |
| `POST` | `/api/auth/registrar-organizador` | chave mestra | Cadastra organizador |
| `POST` | `/api/auth/login` | livre | Faz login (cria sessão) |
| `POST` | `/api/auth/logout` | logado | Encerra a sessão |
| `GET` | `/api/auth/me` | logado | Dados do usuário logado |

**Body do cadastro/login:**
```json
{
  "nome": "Ana Souza",
  "email": "ana@etec.sp.gov.br",
  "senha": "123456",
  "chaveMestra": "ETEC-INTERCLASSE-2026"
}
```
(`chaveMestra` só em `/registrar-organizador`)

### Times (exemplo já implementado)
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/times` | Lista todos os times |
| `GET` | `/api/times?id=1` | Detalhe de um time |
| `POST` | `/api/admin/times` | Cria um time (organizador) |
| `PUT` | `/api/admin/times?id=1` | Atualiza um time (organizador) |
| `DELETE` | `/api/admin/times?id=1` | Remove um time (organizador) |
| `GET` | `/api/classificacao?modalidadeId=1` | Consulta classificação |
| `GET` | `/api/classificacao/csv?modalidadeId=1` | Exporta classificação em CSV |
| `GET` | `/api/jogadores?modalidadeId=1` | Ranking de gols e assistências |
| `POST` | `/api/admin/jogadores` | Cadastra jogador (organizador) |
| `PUT` | `/api/admin/jogadores?id=1` | Atualiza gols e assistências (organizador) |
| `GET` | `/api/configuracoes` | Consulta regras de pontuação |
| `PUT` | `/api/admin/configuracoes` | Altera regras (organizador) |

### Rotas administrativas e consultas adicionais
| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/turmas` | logado | Consulta turmas |
| `POST/PUT/DELETE` | `/api/admin/turmas` | organizador | Escrita de turmas |
| `GET` | `/api/modalidades` | logado | Consulta modalidades |
| `POST/PUT/DELETE` | `/api/admin/modalidades` | organizador | Escrita de modalidades |
| `GET/POST` | `/api/inscricoes` | logado | Consulta / inscreve time |
| `PUT/DELETE` | `/api/admin/inscricoes?id=1` | organizador | Atualiza ou remove inscrição |
| `GET` | `/api/partidas` | logado | Consulta partidas |
| `POST/PUT/PATCH/DELETE` | `/api/admin/partidas` | organizador | Agenda, altera ou remove partida |
| `POST` | `/api/admin/partidas/finalizar?id=1` | organizador | Finaliza partida |
| `GET` | `/api/classificacao?modalidadeId=1` | logado | Tabela de classificação |

---

## 7. Lista de Atividades

Cada atividade é um pedaço do sistema. Sugestão: uma dupla por atividade.

### Fase 1 — Fundamentos
- [x] **1. Criar as tabelas restantes** (`turma`, `modalidade`, `inscricao`, `partida`, `configuracao`)
      com as chaves estrangeiras apontando para as tabelas existentes.
      Dica: adicione o `CREATE TABLE` dentro de `Conexao.criarTabelas`.
- [x] **2. Criar os Models** `Turma`, `Modalidade`, `Inscricao`, `Partida`, `Configuracao`
      (use `Time` e `Usuario` como modelo).
- [x] **3. Criar os DAOs** correspondentes, com os 5 métodos CRUD
      (veja `TimeDao` como referência).

### Fase 2 — CRUDs e rotas
- [x] **4. `TurmaController`** — `/api/turmas` (GET, POST, PUT, DELETE)
- [x] **5. `ModalidadeController`** — `/api/modalidades` (GET, POST, PUT, DELETE),
      incluindo ativar/desativar a modalidade.
- [x] **6. `InscricaoController`** — `/api/inscricoes`:
      - [x] listar todas
      - [x] listar por modalidade (`?modalidadeId=`)
      - [x] inscrever time
      - [x] validação de **1 time por modalidade**
      - [x] alterar status (`PENDENTE` → `CONFIRMADA`/`NEGADA`), somente organizador
- [x] **7. `PartidaController`** — `/api/partidas`:
      - [x] listar com filtros (`?modalidadeId=`, `?status=`, `?data=`)
      - [x] agendar partida
      - [x] validar: mesma modalidade, times confirmados, times diferentes
      - [x] atualizar placar (só se não estiver `FINALIZADA`)
      - [x] finalizar partida (`POST /api/admin/partidas/finalizar?id=1`)

### Fase 3 — Regras avançadas
- [x] **8. `ConfiguracaoDao` + rota** — ler e alterar `PONTOS_VITORIA`, `PONTOS_EMPATE`,
      `PONTOS_DERROTA`, `PERMITE_EMPATE`.
- [x] **9. Classificação dentro de `TimeController`** — `/api/classificacao?modalidadeId=1`:
      calcula pontos, vitórias, saldo e ordena conforme a seção 5.4.
- [x] **10. Validar empate** — se `PERMITE_EMPATE = 0`, bloquear a finalização de partida empatada.
- [x] **11. Proteger as rotas de escrita** — mover o CRUD de organizers para `/api/admin/*`
      (já existe o `OrganizadorFilter`) e deixar as rotas de leitura livres para logados.

### Fase 4 — Acabamento
- [ ] **12. Validações** — nome obrigatório, email válido, campos não vazios,
      mensagens de erro claras em JSON.
- [ ] **13. Documentar** — documentar todas as rotas no README e adicionar exemplos
      de requisição/resposta (curl ou Postman).
- [ ] **14. Testes manuais** — checklist de teste de cada rota (entrada válida, entrada
      inválida, sem permissão, recurso inexistente).

### Bônus
- [x] **15.** Campo de fase para grupos e mata-mata: grupos + mata-mata (`fase` = `GRUPOS`, `QUARTAS`, `SEMI`, `FINAL`).
- [x] **16.** Ranking individual (gols/assistências dos jogadores).
- [x] **17.** Exportar a classificação em CSV.

---

## 8. Convenções

- **Resposta de sucesso:** o objeto JSON direto (`200`).
- **Resposta de erro:** `{"erro": "mensagem"}` com o status HTTP correto
  (`400` requisição inválida, `401` sem login, `403` sem permissão, `404` não encontrado, `409` conflito).
- **Datas:** texto no formato `yyyy-MM-dd` (`2026-11-20`).
- **SQL:** sempre `PreparedStatement`, nunca concatenar string.
- **Controller:** valida a entrada e monta a resposta. Algumas consultas adicionais de classificação e jogadores usam SQL diretamente no `TimeController`, seguindo a implementação simples deste projeto.

## 9. Regras avançadas e rotas adicionais

### Configurações (somente organizador para alterar)
- `GET /api/configuracoes` lista as quatro regras.
- `PUT /api/admin/configuracoes` altera uma regra. Exemplo:

```bash
curl -X PUT http://localhost:8082/api/admin/configuracoes -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"chave":"PONTOS_VITORIA","valor":3}'
```

As chaves aceitas são `PONTOS_VITORIA`, `PONTOS_EMPATE`, `PONTOS_DERROTA` e `PERMITE_EMPATE`. Para `PERMITE_EMPATE`, use 1 ou 0.

### Classificação e CSV
- `GET /api/classificacao?modalidadeId=1` calcula pontos, vitórias e saldo de gols. A ordem é pontos, vitórias, saldo e nome.
- `GET /api/classificacao/csv?modalidadeId=1` baixa `classificacao.csv`.

```bash
curl "http://localhost:8082/api/classificacao?modalidadeId=1"
curl -b cookies.txt -o classificacao.csv "http://localhost:8082/api/classificacao/csv?modalidadeId=1"
```

### Fases e jogadores
As partidas aceitam `fase`: `GRUPOS`, `QUARTAS`, `SEMI` ou `FINAL` (também existe `AVULSO` para partidas sem chaveamento). Se `PERMITE_EMPATE` estiver em 0, a finalização de placar empatado retorna HTTP 409.

- `GET /api/jogadores?modalidadeId=1` mostra o ranking individual por gols e assistências.
- `POST /api/admin/jogadores` cadastra jogador (organizador): `{"nome":"Ana","timeId":1}`.
- `PUT /api/admin/jogadores?id=1` atualiza estatísticas: `{"gols":4,"assistencias":2}`.

Exemplo:
```bash
curl -X POST http://localhost:8082/api/admin/jogadores -H "Content-Type: application/json" -b cookies.txt -d '{"nome":"Ana","timeId":1}'
curl -X PUT "http://localhost:8082/api/admin/jogadores?id=1" -H "Content-Type: application/json" -b cookies.txt -d '{"gols":4,"assistencias":2}'
```

### Proteção administrativa
As operações de escrita administrativa de times, turmas e modalidades usam `/api/admin/times`, `/api/admin/turmas` e `/api/admin/modalidades`. As alterações administrativas de partidas usam `/api/admin/partidas` e `/api/admin/partidas/finalizar`. A remoção administrativa de inscrições usa `/api/admin/inscricoes`. Essas rotas passam pelo `OrganizadorFilter`; as rotas GET originais continuam disponíveis para consulta.

## 10. Checklist de testes manuais

- [ ] `GET /api/configuracoes`: conferir as quatro configurações padrão.
- [ ] `PUT /api/admin/configuracoes`: testar organizador logado; testar participante (403), sem login (401), chave desconhecida (400) e valor negativo (400).
- [ ] `GET /api/classificacao?modalidadeId=1`: testar modalidade existente, parâmetro ausente, texto no lugar do número e modalidade sem jogos.
- [ ] `POST /api/admin/partidas/finalizar?id=1`: testar placar empatado com `PERMITE_EMPATE=0` (409), empate permitido, partida inexistente (404) e partida já finalizada (409).
- [ ] `GET /api/admin/times`: testar sem login (401), participante (403) e organizador (200).
- [ ] `POST /api/admin/times`: testar nome válido, nome vazio e sem permissão.
- [ ] `GET /api/classificacao/csv?modalidadeId=1`: abrir o arquivo em editor de texto ou planilha.
- [ ] `POST /api/admin/jogadores`: testar jogador válido, nome vazio e time inexistente.
- [ ] `PUT /api/admin/jogadores?id=1`: testar estatísticas válidas, valores negativos e jogador inexistente.


## 11. Rotas administrativas atualizadas

| Método | Rota | Acesso | Função |
|---|---|---|---|
| GET | `/api/times` | consulta | Lista ou busca time |
| POST, PUT, DELETE | `/api/admin/times` | organizador | CRUD de times |
| GET | `/api/turmas` | consulta | Lista ou busca turma |
| POST, PUT, DELETE | `/api/admin/turmas` | organizador | CRUD de turmas |
| GET | `/api/modalidades` | consulta | Lista ou busca modalidade |
| POST, PUT, DELETE | `/api/admin/modalidades` | organizador | CRUD de modalidades |
| GET | `/api/partidas` | consulta | Lista partidas e filtros |
| POST, PUT, DELETE | `/api/admin/partidas` | organizador | Agenda, altera ou remove partida |
| POST | `/api/admin/partidas/finalizar?id=1` | organizador | Finaliza partida |
| GET | `/api/inscricoes` | consulta | Lista inscrições |
| POST | `/api/inscricoes` | logado | Inscreve time |
| PUT | `/api/inscricoes?id=1` | organizador para alterar status | Atualiza inscrição |
| DELETE | `/api/admin/inscricoes?id=1` | organizador | Remove inscrição |
| GET | `/api/classificacao?modalidadeId=1` | consulta | Classificação |
| GET | `/api/classificacao/csv?modalidadeId=1` | consulta | Exportação CSV |
| GET | `/api/configuracoes` | consulta | Consulta regras |
| PUT | `/api/admin/configuracoes` | organizador | Altera regras |
| GET | `/api/jogadores` | consulta | Ranking individual |
| POST, PUT | `/api/admin/jogadores` | organizador | Cadastro e atualização de estatísticas |

As consultas são mantidas nas rotas normais. As operações administrativas de escrita usam o prefixo `/api/admin` e são verificadas pelo filtro de organizador.


## Organização dos Controllers

As rotas de configuração ficam no `ApiController`. As consultas de classificação, exportação CSV e ranking de jogadores, além do CRUD de times e jogadores, ficam no `TimeController`. As partidas continuam no `PartidaController`. Não existem Controllers separados para classificação, configuração ou jogadores. As rotas de escrita ficam em `/api/admin/*` e são protegidas pelo `OrganizadorFilter`.
