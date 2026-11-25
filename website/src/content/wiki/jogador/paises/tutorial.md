---
title: "Tutorial: o seu primeiro país"
summary: Passo a passo, com os comandos. Fundar, comprar território, chamar membros, subir de nível e declarar guerra.
order: 1.5
---

Este tutorial vai do zero até um país funcionando. O exemplo é o **Pedro**, que quer fundar **Portugal** em Lisboa.

O que está entre `< >` você troca pelo que quiser. Não digite os sinais.

## Antes de começar

- **Junte 20.000 Geos.** É o preço para fundar um país. Para ganhar dinheiro, veja [Vender e comprar](/wiki/economia/vender-e-comprar/).
- **Vá até o lugar.** O mapa é a Terra e não existe teleporte, então vá até a região onde quer o país.
- **Veja se o lugar está livre.** `/pais map` mostra no chat um mapa dos chunks em volta, com os que já têm dono.

## 1. Fundar o país

Fique no lugar que vai ser o centro do país e digite:

```
/pais create Portugal
```

- Custa **20.000 Geos**, tirados do seu dinheiro.
- O país já nasce com os **9 chunks** em volta de você (um quadrado de 3 × 3), sem custo extra.
- Você vira o **Fundador** e pode tudo no país.
- O nome pode ter de 3 a 32 letras, com espaços.

## 2. Ver as bordas

```
/pais view
```

Mostra as bordas do seu país e dos vizinhos com partículas. Para desligar, use `/pais view disable`.

## 3. Guardar dinheiro no banco

Quase tudo do país é pago pelo **banco do país**, não pelo seu bolso: território, manutenção e níveis.

```
/pais bank deposit 50000
/pais bank balance
/pais bank withdraw 10000
```

- `deposit` guarda o seu dinheiro no banco, `balance` mostra quanto tem e `withdraw` saca.
- Só saca quem tem essa permissão no cargo. O Fundador sempre pode.
- Se o banco não tiver o bastante para comprar um chunk, o jogo tira o que falta do seu dinheiro.

## 4. Comprar território

Cada chunk novo custa **1.000 Geos**.

| Comando | O que faz |
|---|---|
| `/pais claim` | Compra o chunk onde você está |
| `/pais claim auto` | Compra cada chunk em que você entra enquanto anda. Digite de novo para parar |
| `/pais claim radius <número>` | Compra os chunks em volta de você |
| `/pais claim list` | Lista os chunks do país |
| `/pais unclaim` | Devolve o chunk onde você está |

> **Exemplo:** o Pedro digita `/pais claim radius 2` no centro de Lisboa. Isso pega um quadrado de 5 × 5 chunks em volta dele. Ele só paga pelos chunks que o país ainda não tinha.

Antes de comprar muito, lembre que **cada chunk custa 200 Geos por dia** de manutenção, cobrados do banco todo dia às 15:00. Com os 9 chunks iniciais, são 1.800 Geos por dia. Veja [Território e manutenção](/wiki/paises/territorio/).

O país começa com até **100 chunks** e pode ter até **6 partes separadas**: o território principal e 5 colônias.

## 5. Chamar membros

```
/pais trust Maria
```

A Maria recebe um convite no chat. Para aceitar, ela clica no convite ou digita:

```
/pais invites accept Portugal
```

| Comando | O que faz |
|---|---|
| `/pais trust <jogador>` | Convida um jogador para o país |
| `/pais untrust <jogador>` | Tira um jogador do país |
| `/pais invites accept <país>` | Aceita um convite que você recebeu |
| `/pais invites decline <país>` | Recusa um convite |
| `/pais leave` | Sai do país |

Para mudar o cargo de alguém (Civil, Admin), abra o menu do país com `/pais` e vá em membros. Os cargos estão explicados em [O que é um país](/wiki/paises/o-que-e-um-pais/).

## 6. Subir de nível

```
/pais menu land level
```

O menu de níveis mostra quanto custa o próximo nível e o que ele dá. Clique em **Level up** para subir. O dinheiro sai do banco do país, e só o Fundador pode subir de nível.

O nível 2 (Território) custa **30.000 Geos** e aumenta o limite para 150 chunks e 22 membros. A tabela completa está em [Fundar e evoluir](/wiki/paises/fundar-e-evoluir/).

## 7. Aliados e inimigos

| Comando | O que faz |
|---|---|
| `/pais relation ally add <país>` | Pede aliança. O outro país precisa aceitar |
| `/pais relation ally remove <país>` | Desfaz a aliança |
| `/pais relation enemy add <país>` | Marca um país como inimigo |
| `/pais relation enemy remove <país>` | Pede paz a um inimigo |

Os membros de um país aliado recebem o cargo **Aliado** no seu país.

## 8. Declarar guerra

```
/pais war declare Espanha
```

| Comando | O que faz |
|---|---|
| `/pais war declare <país>` | Declara guerra |
| `/pais war info` | Mostra os pontos da guerra |
| `/pais war info captureflag` | Mostra a receita da bandeira de captura |
| `/pais war menu` | Abre o menu da guerra, onde também dá para se render |

A guerra começa **1 hora** depois de declarada. Como se ganha e o que o vencedor leva está em [Guerras](/wiki/paises/guerras/).

## 9. A economia do país

Com o país pronto, ele pode ter **moeda própria**, um **tesouro** e um **mercado**. Isso está na [Wiki do País](/wiki/pais/).

## Resumo dos comandos

| Comando | O que faz |
|---|---|
| `/pais` | Abre o menu do seu país |
| `/pais create <nome>` | Funda um país (20.000 Geos) |
| `/pais map` | Mapa dos chunks em volta |
| `/pais view` | Mostra as bordas |
| `/pais bank deposit <valor>` | Guarda dinheiro no banco |
| `/pais claim` | Compra o chunk onde você está (1.000 Geos) |
| `/pais trust <jogador>` | Convida um jogador |
| `/pais menu land level` | Abre o menu de níveis |
| `/pais war declare <país>` | Declara guerra |

`/lands` é o mesmo que `/pais`: o que vem depois é igual nos dois.
