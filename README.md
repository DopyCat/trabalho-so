# Simulador de Memória Virtual — Algoritmos de Substituição de Páginas

**Desenvolvido por:** Agni Sofia Gomes Chaves  
**Grupo:** Único membro

---

## 📌 Descrição do Projeto

Este projeto implementa um simulador completo dos principais algoritmos de substituição de páginas utilizados em sistemas de memória virtual. O programa realiza a simulação das políticas **FIFO**, **LRU**, **RAND** e **MIN**, permitindo analisar o comportamento de cada uma sob diferentes cenários de requisição de páginas.

---

## 📋 Funcionalidades

- ✅ Cálculo automático dos parâmetros derivados (tamanho da página, quantidade de frames e swap mínimo)
- ✅ Simulação completa das 4 políticas de substituição
- ✅ Contabilização precisa de **page faults** para cada algoritmo
- ✅ Rastreamento das páginas removidas e armazenadas no swap
- ✅ Suporte a múltiplas sequências de requisições de entrada
- ✅ Retorno estruturado e formatado dos resultados

---

## 🔍 Algoritmos Implementados

### 1. **FIFO — First In, First Out**
Remove sempre a página **mais antiga** na memória física. É o algoritmo mais simples, mas pode ser ineficiente para padrões de acesso cíclicos.

- Estrutura: `Queue<Integer>` para manter a ordem de chegada
- Lógica: A primeira página inserida é a primeira a ser removida

### 2. **LRU — Least Recently Used**
Remove a página que ficou **mais tempo sem ser usada**. Oferece melhor desempenho que FIFO em muitos casos reais.

- Estrutura: `LinkedHashSet<Integer>` para manter ordem de acesso
- Lógica: Ao acessar uma página, ela é movida para o final (MRU); a primeira é removida (LRU)

### 3. **RAND — Random Replacement**
Remove uma página **aleatoriamente** quando não há espaço disponível. Simples, mas imprevisível.

- Estrutura: `Set<Integer>` + `Random`
- Lógica: Escolhe um índice aleatório da lista de páginas na memória

### 4. **MIN — Ótimo (Algoritmo de Bélády)**
Remove a página cujo **próximo uso está mais distante no futuro**. É o algoritmo ótimo teoricamente, porém impossível implementar em tempo real (requer conhecimento do futuro).

- Estrutura: `Set<Integer>` com análise de distância
- Lógica: Calcula a distância até a próxima requisição de cada página e remove a com maior distância

---

## 🛠️ Parâmetros Derivados

O programa calcula automaticamente os seguintes parâmetros:

| Parâmetro | Fórmula | Descrição |
|-----------|---------|-----------|
| **Tamanho da Página** | `V / P` | Tamanho em bytes (ou unidade) de cada página |
| **Número de Frames** | `M / tamanhoPagina` | Quantidade de frames que cabem na memória física |
| **Swap Mínimo** | `V - M` | Espaço mínimo necessário em disco para swap |

Onde:
- **M** = Tamanho da memória física
- **V** = Tamanho da memória virtual
- **P** = Número total de páginas

---

### Classes Auxiliares

#### `SimulationResult`
Armazena os resultados de cada simulação:
- `politica`: Nome do algoritmo
- `tempoDecorrido`: Tempo de execução (reservado para futuras melhorias)
- `pageFaults`: Número de page faults ocorridos
- `swapState`: Páginas presentes no swap ao final da simulação

#### `DerivedParameters`
Armazena os parâmetros calculados:
- `tamanhoPagina`: Tamanho de cada página
- `numFrames`: Número de frames disponíveis
- `tamanhoSwapMinimo`: Espaço mínimo necessário para swap

---

## ▶️ Como Executar

### Compilação

```bash
javac Main.java
```

### Execução

```bash
java Main < entrada.txt
```

Ou com redirecionamento de saída:

```bash
java Main < entrada.txt > saida.txt
```

### Formato de Entrada

O programa espera entrada via `stdin` com a seguinte estrutura:

```
M V A P N
R1 página1 página2 ... páginaR1
R2 página1 página2 ... páginaR2
...
RN página1 página2 ... páginaRN
```

Onde:
- **M**: Tamanho da memória física
- **V**: Tamanho da memória virtual
- **A**: Identificação da arquitetura (apenas documentação)
- **P**: Número total de páginas do sistema
- **N**: Número de sequências de requisições
- **Ri**: Número de requisições na sequência *i*
- **páginaX**: Identificador da página requisitada

### Exemplo de Entrada

```
256 1024 x86 4 2
5 1 2 3 1 4
4 1 3 2 1
```

### Formato de Saída

```
<tamanho_da_pagina>
<num_frames>
<swap_minimo>
<num_sequencias>

<sequencia_1>
FIFO
<tempo>
<page_faults>
<swap_state>
RAND
<tempo>
<page_faults>
<swap_state>
LRU
<tempo>
<page_faults>
<swap_state>
MIN
<tempo>
<page_faults>
<swap_state>

<sequencia_2>
...
```

---

## 🧪 Testes

O projeto inclui arquivos de teste:

- **small.txt**: Teste com pequeno volume de requisições
- **medium.txt**: Teste com volume médio
- **large.txt**: Teste com grande volume de requisições

Para executar os testes:

```bash
java Main < small.txt
java Main < medium.txt
java Main < large.txt
```

---

## 💡 Detalhes de Implementação

### Gestão de Page Faults

Cada simulação conta os **page faults** (falhas de página) que ocorrem quando uma página requisitada não está na memória física.

### Estado do Swap

Um conjunto (`Set<Integer>`) rastreia as páginas que foram expulsas para o swap durante a execução. Ao final, essas páginas são ordenadas e formatadas como uma string.

### Desempate no Algoritmo MIN

Quando duas páginas têm a mesma distância até o próximo acesso, o algoritmo escolhe aquela com o **maior identificador** como critério de desempate.

---

## 📊 Comparação dos Algoritmos

| Algoritmo | Complexidade | Característica |
|-----------|--------------|-----------------|
| **FIFO** | O(1) | Determinístico, simples |
| **RAND** | O(n) | Aleatório, imprevisível |
| **LRU** | O(1) amortizado | Baseado em histórico de acesso |
| **MIN** | O(n²) | Ótimo teórico, impossível na prática |

---

## 🔧 Requisitos

- **Java**: JDK 8 ou superior
- **Sistema Operacional**: Windows, Linux ou macOS
- **Memória**: Mínimo 512 MB disponível

---

## 📝 Observações

- O algoritmo **MIN** é chamado de "ótimo" porque fornece o número mínimo possível de page faults, porém requer conhecimento do futuro das requisições.
- O tempo decorrido (`tempoDecorrido`) é sempre 0 nesta implementação, mas a estrutura está preparada para futuras medições de performance.
- O programa valida a entrada e retorna silenciosamente se dados inválidos forem fornecidos.

---

## 📚 Referências

- Tanenbaum, A. S., & Bos, H. (2015). *Modern Operating Systems* (4th ed.).
- Silberschatz, A., Galvin, P. B., & Gagne, G. (2018). *Operating System Concepts* (10th ed.).

---

**Desenvolvido para a disciplina de Sistemas Operacionais**
