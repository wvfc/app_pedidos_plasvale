# Estrutura da planilha `FOLHA DE PEDIDOS TABELA 130 COM IPI 6,5 – SETEMBRO 2026`

Levantamento feito diretamente no arquivo `.xlsx` (valores e fórmulas), usado como base para o aplicativo.

## Abas

| Aba | Conteúdo |
|---|---|
| `Importante` | Avisos: SP desconsidera a coluna de ST; planilha não vale para clientes do Simples Nacional. |
| `FOLHA DE PEDIDO ` | Formulário do pedido: cabeçalho + 308 linhas de itens (21 a 328). |
| `Tabela 130 ` | Catálogo: 557 produtos com preço por prazo de pagamento. |
| `BASE CONTÁBIL` | Alíquotas de ST por NCM e UF + comparativo de MVA/IVA. |

## `Tabela 130 ` — catálogo de preços

- Cabeçalho na **linha 5**, produtos da **linha 6 à 562** (557 itens, sem códigos repetidos).
- Colunas: `A` Referência, `B` Descrição, `C` EMB. (múltiplo de venda), `D..N` preços unitários por prazo.
- Prazos (colunas `D` a `N`): **À Vista, 14, 21, 28, 35, 42, 49, 56, 60, 75, 90** dias.
- As colunas `O..T` (`MG`, `PR`, `RJ`, `RS`, `SP`, `IPI`) existem no cabeçalho mas **estão vazias** no arquivo.

## `FOLHA DE PEDIDO ` — cabeçalho

| Célula | Campo | Validação |
|---|---|---|
| `F6` | UF do cliente | lista em `U2:U22` (21 UFs) |
| `C9` | Tabela | lista em `T2:T5` (só `119` preenchido) |
| `F9` | Prazo de pagamento | lista em `S2:S12` (À Vista … 90) |
| `G8:G10` | Regras comerciais | 20% de desconto; mínimo R$ 3.000 varejo / R$ 5.000 redes |
| `G11` | Observação do pedido | texto livre |
| `C12` | Total | `=SUM($N$21:$N$328)` (soma do **valor líquido**) |

## `FOLHA DE PEDIDO ` — colunas dos itens (linha 20 é o cabeçalho)

| Col. | Campo | Como é calculado na planilha |
|---|---|---|
| `B` | CÓDIGO PRODUTO | digitado |
| `C` | DESCRIÇÃO | `PROCV` do código em `Tabela 130 ` (coluna 2) |
| `D` | M.V. | `PROCV` do código (coluna 3 = embalagem) |
| `E` | COR | digitado |
| `F` | QTDE | digitado |
| `G` | PREÇO TABELA | `PROCV` do código + `CORRESP` do prazo (`F9`) nas colunas de preço |
| `H` | DESC. | fração digitada (ex.: `0,2` = 20%) |
| `I` | PREÇO UNI. | `G - (G * H)` |
| `J` | IPI | percentual do produto (no arquivo a origem está quebrada, `#REF!`); o nome do arquivo fixa **6,5%** |
| `K` | ST | alíquota por UF (`F6`) — origem também `#REF!`, resolvida via `BASE CONTÁBIL!N:O` |
| `L` | PREÇO C/ IPI | `I * (1 + J%)` |
| `M` | PREÇO C/ ST | `L * (1 + K)` |
| `N` | VALOR LÍQ. | `F * I` |
| `O` | VALOR BRUTO | `M * F` |
| `P` | NCM | `PROCV` quebrado (`#REF!`) — **não há NCM por produto no arquivo** |

## `BASE CONTÁBIL`

- `A3:K36`: NCM × UF com alíquotas de ST, em dois blocos — `B:F` = **REVENDA**, `G:K` = **NACIONAL**, ambos para `MG, PR, RJ, RS, SP`.
- `N2:O6`: mapa UF → índice de coluna (`MG=32, PR=33, RJ=34, RS=35, SP=36`) usado pelos `PROCV` da coluna `K` da folha de pedido.
- A partir da linha 40: comparativo de MVA/IVA original e ajustado por NCM/UF, com o custo de ST resultante.
- NCM 3924.10.00 (plásticos de uso doméstico), que cobre a maior parte da linha: MG 25,26%, PR 36,73%, RJ 38,17%, RS 0%, SP 32,31% (revenda).

## Consequências para o aplicativo

1. **IPI**: como a coluna de IPI por produto não veio preenchida, o app usa um percentual único por pedido, com padrão **6,5%** (o do nome do arquivo), editável por pedido e por item.
2. **ST**: sem o NCM por produto, o app pede a alíquota de ST por UF (sugerida a partir do NCM 3924.10.00 da `BASE CONTÁBIL`) e permite ajuste por item. Em **SP a sugestão é 0%**, conforme a aba `Importante`.
3. **UFs**: a lista é a mesma da validação da planilha (21 estados). RS, SC, GO, RN, SE e RR não constam na lista original.
4. **Total do pedido**: a planilha totaliza o valor líquido (`C12`); o app mostra os dois — **líquido** (sem IPI/ST) e **bruto** (com IPI e ST), além dos totais de IPI e de ST.
