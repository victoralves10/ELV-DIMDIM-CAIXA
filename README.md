# 💛 DimDim Caixa — Web App + Azure SQL Database

Projeto do **2º Checkpoint (2º semestre)** da disciplina *DevOps Tools & Cloud Computing* — FIAP.

> Grupo: **NOME_DO_GRUPO** · Vídeo de demonstração: **[LINK_DO_VIDEO](https://youtube.com/)**

---

## 📌 Descrição da solução

A **DimDim** é um banco com mais de 1 milhão de correntistas e **2 mil caixas eletrônicos**, que sofre com um parque tecnológico obsoleto, perda de desempenho no banco de dados e queda de conexão dos clientes. Antes de levar suas aplicações oficialmente para a nuvem, a diretoria pediu à nossa consultoria um **teste em ambiente de nuvem**, usando apenas serviços **PaaS** (sem gerenciar servidores).

Desenvolvemos o **DimDim Caixa**, uma aplicação web em **Java 17 + Spring Boot (MVC) + Thymeleaf** com duas áreas:

| Área | Quem usa | O que faz |
|---|---|---|
| 🏧 **Caixa Eletrônico** | Cliente | Entra com número da conta + PIN e faz **saque**, **depósito** e **consulta de saldo** numa tela que simula um caixa (com teclado numérico) |
| 🏢 **Gerência (back-office)** | Agência | **CRUD completo de Clientes** e **CRUD completo de Contas** (abrir, consultar, alterar e encerrar) |

Toda a solução roda na Microsoft Azure:

- **Azure App Service (Web App)** hospeda a aplicação (`.jar`, Java 17, Linux)
- **Azure SQL Database** guarda os dados (PaaS, não containerizado)
- **Application Insights** monitora a aplicação e as chamadas ao banco (dependências SQL)
- **Azure CLI + GitHub Actions** automatizam a criação dos recursos e o deploy

---

## 🏗️ Arquitetura

![Arquitetura da solução](docs/arquitetura.svg)

**Fluxo:**
1. O grupo roda o script `scripts/deploy-dimdim.sh` no **Azure Cloud Shell**, que cria todos os recursos (Resource Group, SQL Server, Database, Application Insights, App Service Plan e Web App) e configura as variáveis de ambiente.
2. Cada `git push` na branch `main` dispara o **GitHub Actions**, que compila o projeto com Maven e publica o `.jar` no **Web App**.
3. O usuário acessa o Web App por **HTTPS**; a aplicação grava e lê os dados no **Azure SQL Database** via JDBC (TLS, porta 1433).
4. O agente Java do **Application Insights** coleta requisições, tempos de resposta, falhas e as **consultas SQL** feitas ao banco.

---

## 🗄️ Banco de dados

Duas tabelas com relacionamento **1:N** (um cliente possui várias contas). O DDL está em [`scripts/ddl.sql`](scripts/ddl.sql).

```mermaid
erDiagram
    CLIENTE ||--o{ CONTA : possui
    CLIENTE {
        BIGINT id PK
        VARCHAR nome
        CHAR cpf UK
        VARCHAR email
        DATETIME2 data_cadastro
    }
    CONTA {
        BIGINT id PK
        VARCHAR numero UK
        VARCHAR agencia
        VARCHAR tipo "CORRENTE | POUPANCA"
        DECIMAL saldo
        CHAR pin
        DATETIME2 data_abertura
        BIGINT cliente_id FK
    }
```

> Ao excluir um cliente, as contas dele são removidas em cascata (`ON DELETE CASCADE`).

---

## 🧰 Tecnologias

- Java 17, Spring Boot 3.3, Spring MVC, Spring Data JPA, Bean Validation
- Thymeleaf + Bootstrap 5 (front-end renderizado no servidor — **não é API**)
- Driver `mssql-jdbc` (Azure SQL Database)
- Azure App Service (Linux, F1), Azure SQL Database (Basic), Application Insights
- Azure CLI e GitHub Actions

---

## 📁 Estrutura do repositório

```
dimdim-caixa/
├── docs/
│   └── arquitetura.svg          # desenho da arquitetura
├── scripts/
│   ├── deploy-dimdim.sh         # Azure CLI: cria recursos + configura deploy
│   ├── ddl.sql                  # DDL das tabelas
│   └── consultas.sql            # SELECTs para conferir a persistência
├── src/main/java/com/dimdim/caixa/
│   ├── controller/              # Home, Cliente, Conta, Caixa
│   ├── model/                   # Cliente, Conta, TipoConta
│   ├── repository/              # Spring Data JPA
│   └── service/                 # regras de negócio (saque, depósito, validações)
├── src/main/resources/
│   ├── application.properties   # lê credenciais de variáveis de ambiente
│   └── templates/               # telas Thymeleaf
└── pom.xml
```

---

## 🛤️ Rotas da aplicação

| Método | Rota | Descrição |
|---|---|---|
| GET | `/` | Página inicial |
| GET | `/clientes` | Lista clientes |
| GET | `/clientes/novo` | Formulário de novo cliente |
| POST | `/clientes` | **Create** cliente |
| GET | `/clientes/{id}` | **Read** cliente (com as contas dele) |
| GET | `/clientes/{id}/editar` | Formulário de edição |
| POST | `/clientes/{id}` | **Update** cliente |
| POST | `/clientes/{id}/excluir` | **Delete** cliente |
| GET | `/contas` | Lista contas |
| GET | `/contas/nova` | Formulário de abertura de conta |
| POST | `/contas` | **Create** conta |
| GET | `/contas/{id}` | **Read** conta |
| GET | `/contas/{id}/editar` | Formulário de edição |
| POST | `/contas/{id}` | **Update** conta |
| POST | `/contas/{id}/excluir` | **Delete** conta |
| GET | `/caixa` | Tela de login do caixa (conta + PIN) |
| POST | `/caixa/entrar` | Autentica no caixa |
| GET | `/caixa/menu` | Saldo e operações |
| POST | `/caixa/operacao` | Saque ou depósito (atualiza o saldo) |
| GET | `/caixa/sair` | Encerra a sessão do caixa |

> Como a solução tem **front-end** (não é uma API REST), não há JSON de GET/POST/PUT/DELETE: todas as operações são feitas pelas telas.

---

## 🚀 How to — implantação completa na nuvem

### Pré-requisitos

- Conta na **Azure** com permissão para criar recursos, numa região permitida pela Policy da FIAP: `southcentralus`, `brazilsouth`, `chilecentral`, `mexicocentral` ou `southafricanorth`
- Conta no **GitHub**
- Acesso ao **Azure Cloud Shell** (Bash) em https://shell.azure.com

### 1. Criar o seu repositório no GitHub

1. Faça **fork** deste repositório (ou crie um repositório novo e envie o código).
2. Anote o nome no formato `usuario/repositorio` (ex.: `fulano/dimdim-caixa`).

### 2. Clonar o projeto no Cloud Shell

```bash
git clone https://github.com/victoralves10/ELV-DIMDIM-CAIXA.git
cd ELV-DIMDIM-CAIXA/scripts
```

### 3. Ajustar as variáveis do script

Abra o script e altere **RM**, **LOCATION** e **GITHUB_REPO_NAME** no topo:

```bash
code deploy-dimdim.sh
```

```bash
RM="rm561833"
LOCATION="southafricanorth"
GITHUB_REPO_NAME="victoralves10/ELV-DIMDIM-CAIXA"
```

### 4. Executar o script de criação dos recursos

```bash
bash deploy-dimdim.sh
```

O script vai:

1. Pedir a **senha do administrador do SQL Server** (digitada no terminal, nunca gravada no repositório — use letras maiúsculas, minúsculas, números e símbolo, com no mínimo 8 caracteres).
2. Registrar os providers e criar o **Resource Group**.
3. Criar o **Azure SQL Server** e o banco **db-dimdim** (Basic).
4. Liberar no firewall os **serviços do Azure** (para o Web App) e o **IP do Cloud Shell**.
5. Criar as tabelas executando o `ddl.sql`.
6. Criar o **Application Insights**, o **App Service Plan** (F1, Linux) e o **Web App** (Java 17).
7. Configurar as **App Settings** do Web App: string de conexão do Application Insights e `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.
8. Conectar o Web App ao Application Insights.
9. Configurar o **GitHub Actions** (vai pedir para você autorizar o acesso ao GitHub pelo navegador).

> 💡 Se o Cloud Shell não tiver `sqlcmd`, o script avisa: abra **Portal Azure → SQL databases → db-dimdim → Query editor**, faça login com o usuário `user-dimdim` e a senha, cole o conteúdo de `scripts/ddl.sql` e clique em **Run**.

### 5. Acompanhar o deploy automático

1. No GitHub, abra a aba **Actions** do repositório.
2. O workflow criado pelo script (`.github/workflows/...yml`) compila o projeto com Maven e publica o `.jar` no Web App.
3. Aguarde o job ficar verde ✅. A partir de agora, todo `git push` na `main` faz um novo deploy.

> Se o workflow não começar sozinho, faça um commit qualquer (ex.: editar o README) e dê `git push`.

### 6. Acessar a aplicação

```
https://dimdim-caixa-rm561833.azurewebsites.net
```

> No plano F1 a primeira abertura pode levar até 1 minuto (o app "acorda").

### 7. Testar o CRUD e conferir a persistência no banco

Abra o **Query editor** do `db-dimdim` no portal e use os SELECTs de [`scripts/consultas.sql`](scripts/consultas.sql) **depois de cada operação**:

| # | Operação na aplicação | Conferir no banco |
|---|---|---|
| 1 | **Clientes → Novo cliente**: Maria da Silva, CPF 12345678901, maria@email.com | `SELECT * FROM dbo.cliente;` |
| 2 | **Clientes → Editar**: trocar o email | `SELECT * FROM dbo.cliente;` |
| 3 | **Contas → Abrir conta**: titular Maria, agência 0001, conta 12345, Corrente, PIN 1234, saldo 500 | `SELECT * FROM dbo.conta;` |
| 4 | **Contas → Editar**: mudar para Poupança | `SELECT * FROM dbo.conta;` |
| 5 | **Caixa Eletrônico**: conta 12345 + PIN 1234 → **Depósito** de 200 | saldo = 700,00 |
| 6 | **Caixa Eletrônico** → **Saque** de 50 | saldo = 650,00 |
| 7 | **Clientes → Detalhes**: ver as contas do cliente (relacionamento) | SELECT com `JOIN` |
| 8 | **Contas → Encerrar** | a linha some de `dbo.conta` |
| 9 | **Clientes → Excluir** | a linha some de `dbo.cliente` (e as contas, em cascata) |

### 8. Ver o monitoramento no Application Insights

No portal: **Application Insights → ai-dimdim-caixa**

- **Live Metrics**: requisições acontecendo em tempo real enquanto você usa o app
- **Application map**: Web App → banco `db-dimdim` (dependência SQL)
- **Performance**: tempo de cada rota (`POST /clientes`, `POST /caixa/operacao`...)
- **Transaction search**: cada requisição com as consultas SQL executadas
- **Failures**: erros, se houver

> Os dados podem levar de 2 a 5 minutos para aparecer.

### 9. Limpar os recursos (depois da entrega)

```bash
az group delete --name rg-dimdim-caixa --yes --no-wait
```

---

## 🔐 Segurança

- Nenhuma credencial fica no código: o `application.properties` lê `SPRING_DATASOURCE_*` de variáveis de ambiente, configuradas nas **App Settings** do Web App.
- A senha do banco é digitada no Cloud Shell na hora de rodar o script.
- O firewall do SQL Server libera só os serviços do Azure e o IP do Cloud Shell (sem `0.0.0.0–255.255.255.255`).

---

## 🫂 Integrantes

| RM | Nome |
|---|---|
| RM000000 | Nome do integrante |
| RM000000 | Nome do integrante |
| RM000000 | Nome do integrante |
