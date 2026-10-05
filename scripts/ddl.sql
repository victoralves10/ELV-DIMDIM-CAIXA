-- =====================================================================
--  Projeto DimDim - Caixa Eletrônico
--  DDL - Azure SQL Database (T-SQL)
--  Relacionamento: cliente (1) ---- (N) conta
-- =====================================================================

IF OBJECT_ID('dbo.conta', 'U') IS NOT NULL DROP TABLE dbo.conta;
IF OBJECT_ID('dbo.cliente', 'U') IS NOT NULL DROP TABLE dbo.cliente;

CREATE TABLE dbo.cliente (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    cpf           CHAR(11)     NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL,
    data_cadastro DATETIME2    NOT NULL DEFAULT SYSDATETIME()
);

CREATE TABLE dbo.conta (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    numero       VARCHAR(10)   NOT NULL UNIQUE,
    agencia      VARCHAR(4)    NOT NULL DEFAULT '0001',
    tipo         VARCHAR(20)   NOT NULL CHECK (tipo IN ('CORRENTE', 'POUPANCA')),
    saldo        DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
    pin          CHAR(4)       NOT NULL,
    data_abertura DATETIME2    NOT NULL DEFAULT SYSDATETIME(),
    cliente_id   BIGINT        NOT NULL,
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id)
        REFERENCES dbo.cliente(id) ON DELETE CASCADE
);
