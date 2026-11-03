import assert from "node:assert/strict";
import { describe, test } from "node:test";
import {
  formatarCartaoAtualizado,
  formatarCartaoCadastrado,
  formatarData,
  formatarGasto,
  formatarValor,
  mensagens,
  tecladoTelefone,
} from "../src/messages.js";
import { cartao, gasto } from "./support/dados.js";

describe("formatação das mensagens", () => {
  test("valores saem em reais no padrão brasileiro", () => {
    assert.equal(formatarValor(1234.5), "R$ 1.234,50");
    assert.equal(formatarValor(0.1), "R$ 0,10");
    assert.equal(formatarValor(9999999999.99), "R$ 9.999.999.999,99");
  });

  test("datas saem como dd/mm/aaaa", () => {
    assert.equal(formatarData("2026-10-03"), "03/10/2026");
    assert.equal(formatarData("2027-01-31"), "31/01/2027");
  });

  test("gasto simples mostra categoria, valor, data e descrição, sem linha de cartão", () => {
    const texto = formatarGasto(gasto({ amount: 45, occurredOn: "2026-10-13", description: "Compras do mês" }));

    assert.equal(
      texto,
      [
        "✅ Gasto anotado!",
        "📂 Categoria: Mercado",
        "💰 Valor: R$ 45,00",
        "📅 Data: 13/10/2026",
        "📝 Compras do mês",
      ].join("\n"),
    );
  });

  test("gasto no cartão mostra o mês da fatura por extenso", () => {
    const texto = formatarGasto(gasto({ creditCardId: "c1", creditCardName: "Nubank", billingMonth: "2026-11-01" }));

    assert.match(texto, /💳 Cartão: Nubank \(fatura de novembro de 2026\)/);
  });

  test("compra parcelada mostra o total, as parcelas e a fatura da primeira parcela", () => {
    const texto = formatarGasto(
      gasto({
        amount: 100,
        creditCardId: "c1",
        creditCardName: "Inter",
        billingMonth: "2027-01-01",
        installment: { planId: "p1", number: 1, count: 6, totalAmount: 600 },
      }),
    );

    assert.match(texto, /💰 Valor: R\$ 600,00 em 6x de R\$ 100,00/);
    assert.match(texto, /💳 Cartão: Inter \(1ª parcela na fatura de janeiro de 2027\)/);
  });

  test("cartão sem mês de fatura não gera linha de cartão pela metade", () => {
    const texto = formatarGasto(gasto({ creditCardId: "c1", creditCardName: "Nubank", billingMonth: null }));

    assert.doesNotMatch(texto, /Cartão/);
  });

  test("teclado do telefone tem um único botão que compartilha o contato", () => {
    const teclado = tecladoTelefone();

    assert.deepEqual(teclado.keyboard, [[{ text: "📱 Compartilhar meu número", request_contact: true }]]);
    assert.equal(teclado.one_time_keyboard, true);
    assert.equal(teclado.resize_keyboard, true);
  });

  test("boas-vindas chamam a pessoa pelo nome e explicam para que serve o telefone", () => {
    const texto = mensagens.boasVindas("Ana");

    assert.match(texto, /^Oi, Ana!/);
    assert.match(texto, /sem precisar de senha/);
  });
});

describe("mensagens de cartão", () => {
  test("cartão cadastrado mostra nome, dias e uma dica de uso, sem limite quando não tem", () => {
    const texto = formatarCartaoCadastrado(cartao({ name: "Nubank", closingDay: 3, dueDay: 10, creditLimit: null }));

    assert.match(texto, /cadastrad/i);
    assert.match(texto, /Nubank/);
    assert.match(texto, /Fecha dia 3 e vence dia 10/);
    assert.doesNotMatch(texto, /Limite/);
    assert.match(texto, /no Nubank/);
  });

  test("cartão com limite mostra o limite em reais", () => {
    const texto = formatarCartaoCadastrado(cartao({ creditLimit: 5000 }));

    assert.match(texto, /Limite: R\$ 5\.000,00/);
  });

  test("cartão atualizado avisa que os gastos já lançados continuam na fatura em que estavam", () => {
    const texto = formatarCartaoAtualizado(cartao({ name: "Itaú", closingDay: 20, dueDay: 27, creditLimit: 3500 }));

    assert.match(texto, /atualizad/i);
    assert.match(texto, /Itaú/);
    assert.match(texto, /Fecha dia 20 e vence dia 27/);
    assert.match(texto, /Limite: R\$ 3\.500,00/);
    assert.match(texto, /continuam na fatura/);
    assert.doesNotMatch(texto, /cadastrad/i);
  });

  test("cartão atualizado sem limite não mostra linha de limite", () => {
    const texto = formatarCartaoAtualizado(cartao({ creditLimit: null }));

    assert.match(texto, /Fecha dia 3 e vence dia 10/);
    assert.doesNotMatch(texto, /Limite/);
  });

  test("boas-vindas, telefone salvo e dica de comandos ensinam a cadastrar um cartão", () => {
    for (const texto of [mensagens.bemVindoDeVolta("Ana"), mensagens.telefoneSalvo, mensagens.semComandos]) {
      assert.match(texto, /cart[ãa]o/i);
      assert.match(texto, /fecha dia \d+/);
      assert.match(texto, /vence dia \d+/);
    }
  });
});
