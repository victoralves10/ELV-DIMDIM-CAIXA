#!/bin/bash
# =====================================================================
#  Projeto DimDim - Caixa Eletrônico (Spring Boot + Thymeleaf)
#  Web App (PaaS) + Azure SQL Database (PaaS) + Application Insights
#  Deploy automatizado: Azure CLI + GitHub Actions
#
#  Executar no Azure Cloud Shell (Bash)
#  ANTES DE RODAR: altere RM, LOCATION e GITHUB_REPO_NAME
# =====================================================================

set -e

# ---------- Variáveis (ALTERE AQUI) ----------
RM="rm561833"
LOCATION="southafricanorth"                     # permitidas: southcentralus, brazilsouth, chilecentral, mexicocentral, southafricanorth
GITHUB_REPO_NAME="victoralves10/ELV-DIMDIM-CAIXA"
BRANCH="main"

RESOURCE_GROUP_NAME="rg-dimdim-caixa"
APP_SERVICE_PLAN="plan-dimdim-caixa"
WEBAPP_NAME="dimdim-caixa-$RM"
RUNTIME="JAVA:17-java17"
APP_INSIGHTS_NAME="ai-dimdim-caixa"

SQL_SERVER_NAME="sql-server-dimdim-$RM"
SQL_DB_NAME="db-dimdim"
SQL_ADMIN_USER="user-dimdim"

# Senha lida no terminal: NUNCA deixe a senha escrita no repositório
read -s -p "Digite a senha do admin do SQL Server: " SQL_ADMIN_PASSWORD
echo

# ---------- Providers e extensões ----------
az provider register --namespace Microsoft.Web
az provider register --namespace Microsoft.Sql
az provider register --namespace Microsoft.Insights
az provider register --namespace Microsoft.OperationalInsights
az extension add --name application-insights --upgrade --only-show-errors

# ---------- Grupo de Recursos ----------
az group create --name $RESOURCE_GROUP_NAME --location "$LOCATION"

# ---------- Azure SQL Server + Database ----------
az sql server create \
  --name $SQL_SERVER_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --location "$LOCATION" \
  --admin-user $SQL_ADMIN_USER \
  --admin-password "$SQL_ADMIN_PASSWORD" \
  --enable-public-network true

az sql db create \
  --resource-group $RESOURCE_GROUP_NAME \
  --server $SQL_SERVER_NAME \
  --name $SQL_DB_NAME \
  --service-objective Basic \
  --backup-storage-redundancy Local \
  --zone-redundant false

# Firewall: permite que serviços do Azure (o Web App) acessem o banco
az sql server firewall-rule create \
  --resource-group $RESOURCE_GROUP_NAME \
  --server $SQL_SERVER_NAME \
  --name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Firewall: libera o IP atual (Cloud Shell) para rodar o DDL
MEU_IP=$(curl -s https://api.ipify.org)
az sql server firewall-rule create \
  --resource-group $RESOURCE_GROUP_NAME \
  --server $SQL_SERVER_NAME \
  --name AllowCloudShell \
  --start-ip-address $MEU_IP \
  --end-ip-address $MEU_IP

# ---------- Criação das tabelas (DDL) ----------
# Rode este script de dentro da pasta scripts/ (o ddl.sql fica ao lado dele)
if command -v sqlcmd >/dev/null 2>&1; then
  sqlcmd -S "$SQL_SERVER_NAME.database.windows.net" \
         -d $SQL_DB_NAME \
         -U $SQL_ADMIN_USER \
         -P "$SQL_ADMIN_PASSWORD" \
         -i ./ddl.sql
else
  echo ">> sqlcmd não encontrado: cole o conteúdo de ddl.sql no Query Editor do banco no portal Azure"
fi

# ---------- Application Insights ----------
az monitor app-insights component create \
  --app $APP_INSIGHTS_NAME \
  --location "$LOCATION" \
  --resource-group $RESOURCE_GROUP_NAME \
  --application-type web

# ---------- Plano de Serviço + Web App ----------
az appservice plan create \
  --name $APP_SERVICE_PLAN \
  --resource-group $RESOURCE_GROUP_NAME \
  --location "$LOCATION" \
  --sku F1 \
  --is-linux

az webapp create \
  --name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --plan $APP_SERVICE_PLAN \
  --runtime "$RUNTIME"

# Habilita autenticação básica (SCM) - necessária para o deploy
az resource update \
  --resource-group $RESOURCE_GROUP_NAME \
  --namespace Microsoft.Web \
  --resource-type basicPublishingCredentialsPolicies \
  --name scm \
  --parent sites/$WEBAPP_NAME \
  --set properties.allow=true

# ---------- Variáveis de ambiente do App (credenciais ficam só no Azure) ----------
CONNECTION_STRING=$(az monitor app-insights component show \
  --app $APP_INSIGHTS_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --query connectionString \
  --output tsv)

JDBC_URL="jdbc:sqlserver://$SQL_SERVER_NAME.database.windows.net:1433;database=$SQL_DB_NAME;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;"

az webapp config appsettings set \
  --name "$WEBAPP_NAME" \
  --resource-group "$RESOURCE_GROUP_NAME" \
  --settings \
    APPLICATIONINSIGHTS_CONNECTION_STRING="$CONNECTION_STRING" \
    ApplicationInsightsAgent_EXTENSION_VERSION="~3" \
    XDT_MicrosoftApplicationInsights_Mode="Recommended" \
    XDT_MicrosoftApplicationInsights_PreemptSdk="1" \
    SPRING_DATASOURCE_URL="$JDBC_URL" \
    SPRING_DATASOURCE_USERNAME="$SQL_ADMIN_USER" \
    SPRING_DATASOURCE_PASSWORD="$SQL_ADMIN_PASSWORD"

az monitor app-insights component connect-webapp \
  --app $APP_INSIGHTS_NAME \
  --web-app $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP_NAME

az webapp restart --name $WEBAPP_NAME --resource-group $RESOURCE_GROUP_NAME

# ---------- Deploy automatizado com GitHub Actions ----------
# O repositório precisa existir no GitHub com o código já enviado (git push)
# Cada novo push na branch main dispara build + deploy automaticamente
# --force sobrescreve o workflow se ele já existir (ex.: rodando o script pela 2ª vez)
az webapp deployment github-actions add \
  --name $WEBAPP_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --repo $GITHUB_REPO_NAME \
  --branch $BRANCH \
  --login-with-github \
  --force

echo "======================================================"
echo " App: https://$WEBAPP_NAME.azurewebsites.net"
echo "======================================================"

# ---------- Limpeza (rodar só no final) ----------
# az group delete --name $RESOURCE_GROUP_NAME --yes --no-wait