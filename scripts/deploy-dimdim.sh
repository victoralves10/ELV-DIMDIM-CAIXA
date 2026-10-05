#!/bin/bash
# =====================================================================
#  Projeto DimDim - Web App + Azure SQL Database + Application Insights
#  Executar no Azure Cloud Shell (Bash)
# =====================================================================

# ------ VARIAVEIS DE AMBIENTE
RESOURCE_GROUP_NAME="rg-dimdim"
SQL_SERVER_NAME="sql-server-dimdim-rm561713-southafricanorth"
SQL_DB_NAME="db-dimdim"
WEBAPP_NAME="dimdim-caixa-rm561713"
APP_SERVICE_PLAN="dimdim-caixa"
LOCATION="southafricanorth"
RUNTIME="JAVA:17-java17"
APP_INSIGHTS_NAME="ai-dimdim-caixa"

# ------ CRIAÇÃO DO GRUPO DE RECURSOS
az group create --name $RESOURCE_GROUP_NAME --location $LOCATION

# ------ BANCO DE DADOS

# Registrar o serviço que usaremos
az provider register --namespace Microsoft.Sql

# Criação do SQL DATABASE SERVER (SQL Server)
az sql server create \
--name $SQL_SERVER_NAME \
--resource-group $RESOURCE_GROUP_NAME \
--location $LOCATION \
--admin-user user-dimdim \
--admin-password 'Fiap@2tdsvms' \
--enable-public-network true

# Criação da SQL Database
az sql db create \
--resource-group $RESOURCE_GROUP_NAME \
--server $SQL_SERVER_NAME \
--name $SQL_DB_NAME \
--service-objective Basic \
--backup-storage-redundancy Local \
--zone-redundant false

# Criação do firewall do SQL Database Server
az sql server firewall-rule create \
--resource-group $RESOURCE_GROUP_NAME \
--server $SQL_SERVER_NAME \
--name liberaGeral \
--start-ip-address 0.0.0.0 \
--end-ip-address 255.255.255.255

# ----- APLICAÇÃO BACKEND E FRONTEND

# Registrar os serviços que usaremos
az provider register --namespace Microsoft.Web
az provider register --namespace Microsoft.Insights
az provider register --namespace Microsoft.OperationalInsights
az extension add --name application-insights

# Definir Variáveis para os scripts
WEBAPP_NAME="dimdim-caixa-rm561713"
APP_SERVICE_PLAN="dimdim-caixa"
LOCATION="southafricanorth"
RUNTIME="JAVA:17-java17"
BRANCH="main"
APP_INSIGHTS_NAME="ai-dimdim-caixa"

# Criar Application Insights
az monitor app-insights component create \
--app $APP_INSIGHTS_NAME \
--location "$LOCATION" \
--resource-group $RESOURCE_GROUP_NAME \
--application-type web

# Criar o Plano de Serviço
az appservice plan create \
--name $APP_SERVICE_PLAN \
--resource-group $RESOURCE_GROUP_NAME \
--location "$LOCATION" \
--sku F1 \
--is-linux

# Criar o Serviço de Aplicativo
az webapp create \
--name $WEBAPP_NAME \
--resource-group $RESOURCE_GROUP_NAME \
--plan $APP_SERVICE_PLAN \
--runtime "$RUNTIME"

# git clone
cd ~
git clone https://github.com/victoralves10/ELV-DIMDIM-CAIXA.git

# Acessar na pasta do projeto
cd ELV-DIMDIM-CAIXA

# Compilar seu Projeto
mvn clean package

# Entrar na pasta target
cd target

# ------- MONITORAMENTO

CONNECTION_STRING=$(az monitor app-insights component show \
--app $APP_INSIGHTS_NAME \
--resource-group $RESOURCE_GROUP_NAME \
--query connectionString \
--output tsv)

# Configuração do Monitoramento
az webapp config appsettings set --name $WEBAPP_NAME --resource-group $RESOURCE_GROUP_NAME --settings \
APPLICATIONINSIGHTS_CONNECTION_STRING="$CONNECTION_STRING" \
ApplicationInsightsAgent_EXTENSION_VERSION="~3" \
XDT_MicrosoftApplicationInsights_Mode="Recommended" \
XDT_MicrosoftApplicationInsights_PreemptSdk="1" \
SPRING_DATASOURCE_USERNAME="user-dimdim" \
SPRING_DATASOURCE_PASSWORD="Fiap@2tdsvms" \
SPRING_DATASOURCE_URL="jdbc:sqlserver://$SQL_SERVER_NAME.database.windows.net:1433;database=db-dimdim;encrypt=true;loginTimeout=30;"

# Criar a conexão do nosso Web App com o Application Insights
az monitor app-insights component connect-webapp \
--app $APP_INSIGHTS_NAME \
--web-app $WEBAPP_NAME \
--resource-group $RESOURCE_GROUP_NAME

# ---------------- DEPLOY

# Realizar o Deploy
az webapp deploy \
--resource-group $RESOURCE_GROUP_NAME \
--name $WEBAPP_NAME \
--src-path ./dimdim-caixa.jar \
--type jar