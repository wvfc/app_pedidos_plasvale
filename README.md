# Pedidos Plasvale — aplicativo Android offline

Aplicativo Android nativo (Kotlin) que substitui a planilha
*FOLHA DE PEDIDOS TABELA 130 COM IPI 6,5 – SETEMBRO 2026*: monta o pedido no celular,
calcula exatamente como a planilha e **gera o PDF salvo no próprio aparelho**.

Funciona **100% offline**: o catálogo fica embarcado no APK e nenhum dado sai do celular.

## O que o app faz

- **Catálogo embarcado**: 557 produtos da aba `Tabela 130 `, com preço para cada um dos
  11 prazos de pagamento (À Vista, 14, 21, 28, 35, 42, 49, 56, 60, 75 e 90 dias).
- **Busca por código ou descrição** ao adicionar um item.
- **Cálculo idêntico ao da planilha** (veja `docs/ESTRUTURA_PLANILHA.md`):
  - `Preço uni. = Preço tabela − (Preço tabela × Desc.)`
  - `Preço c/ IPI = Preço uni. × (1 + IPI%)`
  - `Preço c/ ST = Preço c/ IPI × (1 + ST%)`
  - `Valor líquido = Qtde × Preço uni.` · `Valor bruto = Qtde × Preço c/ ST`
- **Troca de prazo re-precifica o pedido inteiro** (usa a coluna de preço correspondente).
- **Avisos da planilha** na tela: SP desconsidera ST, pedido mínimo de R$ 3.000/R$ 5.000,
  tabela não válida para Simples Nacional, e alerta quando a quantidade não é múltiplo da
  embalagem (M.V.).
- **PDF do pedido** em A4 paisagem, com cabeçalho do cliente, condições comerciais, tabela
  completa de itens (as mesmas colunas da planilha), totais de líquido/IPI/ST/bruto,
  observações e linhas de assinatura. Várias páginas quando necessário.
- **Salvamento interno**: os pedidos ficam em `filesDir/pedidos.json` (memória interna,
  privada do app). O PDF é salvo em `Android/data/br.com.plasvale.pedidos/files/Documents/`
  e, no Android 10+, copiado também para `Downloads/Pedidos Plasvale/`. Há botão de
  compartilhar (WhatsApp, e-mail etc.) e de exportar backup dos pedidos em JSON.
- **Configurações**: dados do representante, IPI padrão, desconto padrão, UF e prazo padrão
  e a alíquota de ST por UF.

## Como gerar o APK pelo GitHub (sem instalar nada no computador)

O repositório já vem com dois fluxos do GitHub Actions:

**1. A cada push — `Gerar APK` (`.github/workflows/build-apk.yml`)**

Vá em **Actions ▸ Gerar APK**, abra a execução mais recente e baixe o artefato
**`pedidos-plasvale-debug`** (um `.zip` com o `app-debug.apk` dentro). Também dá para
rodar sob demanda pelo botão **Run workflow**.

**2. Para distribuir — `Publicar APK em Release` (`.github/workflows/release-apk.yml`)**

```bash
git tag v1.0.0
git push origin v1.0.0
```

O APK é compilado e anexado a uma **Release** do repositório, com link direto para baixar
no celular. Também pode ser disparado manualmente em **Actions ▸ Publicar APK em Release ▸
Run workflow**, informando o nome da versão.

> O APK gerado é de **debug** (assinado com a chave de depuração do Android): instala
> normalmente no aparelho, basta autorizar "instalar de fontes desconhecidas". Para publicar
> na Play Store é preciso um APK/AAB de release assinado com chave própria.

## Como compilar na sua máquina

Requisitos: Android Studio (Ladybug ou mais recente) ou o SDK de linha de comando, JDK 17.

```bash
git clone <este repositório>
cd app_pedidos_plasvale
./gradlew assembleDebug        # APK em app/build/outputs/apk/debug/
```

No Android Studio: *Open* → selecionar a pasta do projeto → *Run*.
Para gerar o APK assinado: *Build ▸ Generate Signed App Bundle / APK*.

- `compileSdk`/`targetSdk` 34, `minSdk` 24 (Android 7.0+), Gradle 8.9, AGP 8.7.2, Kotlin 1.9.24.
- Dependências: apenas AndroidX + Material. O PDF usa a API nativa `android.graphics.pdf`.
- **Sem permissões perigosas** no manifesto: nada de internet, câmera ou armazenamento externo.

> Observação: o ambiente onde o projeto foi escrito não tem o Android SDK (o download do SDK
> e do Google Maven é bloqueado lá), por isso a compilação é feita pelo GitHub Actions ou
> localmente no Android Studio.

## Atualizar a tabela de preços do mês seguinte

O catálogo vem do arquivo `app/src/main/assets/catalogo.json`, gerado a partir do `.xlsx`:

```bash
pip install openpyxl
python3 tools/gerar_catalogo.py "FOLHA DE PEDIDOS TABELA 130 ... .xlsx"
```

O script lê as abas `Tabela 130 `, `FOLHA DE PEDIDO ` e `BASE CONTÁBIL`, e reescreve o JSON.
Depois é só recompilar o APK.

## Estrutura do código

```
app/src/main/
├── assets/catalogo.json                 # catálogo + prazos + UFs + ST por NCM
├── java/br/com/plasvale/pedidos/
│   ├── model/      Produto, Pedido, ItemPedido (fórmulas da planilha)
│   ├── data/       Catalogo (assets), PedidoRepository (JSON interno), Configuracao
│   ├── pdf/        PdfPedidoGenerator (android.graphics.pdf)
│   ├── ui/         MainActivity, PedidoActivity, ConfiguracaoActivity, SeletorProduto
│   └── util/       Formato (moeda/percentual/datas em pt-BR)
└── res/            layouts, tema, ícone
```

Documentação da planilha original: [`docs/ESTRUTURA_PLANILHA.md`](docs/ESTRUTURA_PLANILHA.md).

## Pontos que dependem de informação que não está na planilha

1. **IPI por produto** — a coluna `J` da folha de pedido aponta para uma referência quebrada
   (`#REF!`). O app usa 6,5% (valor do nome do arquivo) como padrão editável.
2. **ST por produto** — a planilha calcula a ST pelo NCM do item, mas a coluna de NCM do
   catálogo veio vazia. O app usa a alíquota por UF (sugerida pelo NCM 3924.10.00 da
   `BASE CONTÁBIL`, o da maior parte da linha), editável no pedido e em cada item.
