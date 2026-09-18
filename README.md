# Lembrete de Remédios (MediControl)

App Android **100% offline** de controle e lembrete de medicamentos, em Kotlin + Jetpack Compose (Material 3) + Room. Sem anúncios, sem assinaturas, sem backend.

## Stack

- Kotlin 2.0.21 · Jetpack Compose (Material 3) com **Material You** (cores dinâmicas no Android 12+) e suporte automático a tema claro/escuro
- Room 2.6.1 (KSP) para persistência local
- Navigation Compose
- `AlarmManager.setExactAndAllowWhileIdle()` + `BroadcastReceiver` para os lembretes
- `minSdk 26` (Android 8.0) — permite usar `java.time` nativamente, sem desugaring

## Estrutura

```
app/src/main/java/com/medicontrol/app/
├── MainActivity.kt, MediControlApp.kt
├── alarm/            # AlarmScheduler, AlarmReceiver, DoseActionReceiver, BootReceiver, NotificationHelper
├── data/
│   ├── entity/        # Medication, DoseRecord (Room @Entity)
│   ├── dao/            # MedicationDao, DoseRecordDao
│   ├── db/              # AppDatabase, Converters (TypeConverters)
│   ├── model/           # Enums (RecurrenceType, DoseStatus, ...) e DoseUiModel
│   ├── backup/           # BackupData (DTOs serializáveis) + BackupManager (export/import via SAF)
│   └── repository/      # MedicationRepository (une Room + AlarmManager)
├── ui/
│   ├── theme/            # Color/Theme/Type — Material You + dark/light
│   ├── components/       # MedicationIconView (Canvas), DaySelector, Icon/ColorPickerRow
│   ├── home/              # Tela principal
│   ├── addedit/           # Cadastro/edição de medicamento
│   ├── backup/            # Tela de exportar/restaurar backup
│   └── navigation/        # NavHost
└── util/                    # Formatação de datas/horas
```

## Funcionalidades

- Cadastro de medicamento com ícone (Canvas), cor, dosagem, período de tratamento (uso contínuo ou com data de término) e **cadência**: todos os dias, dias específicos da semana, a cada X dias, ou a cada X horas (ex.: antibiótico de 8 em 8h).
- Home com barra de dias, indicador de adesão (dias com dose não marcada ficam em vermelho) e badge de "Última dose!" no dia de término.
- Lembretes exatos (`AlarmManager.setExactAndAllowWhileIdle`) que sobrevivem a Doze Mode e reboot.
- **Notificação agrupada por horário**: remédios com o mesmo horário caem numa única notificação — 1 remédio usa "Tomei"/"Pular"; 2 ou mais usam "Marcar todos como tomados"/"Pular todos" e listam cada um. Botão de soneca (10 min) em ambos os casos.
- **Controle de estoque** opcional por medicamento: decrementa a cada dose tomada (e desfaz ao desmarcar), com aviso visual de estoque baixo na Home e no cadastro.
- **Backup/restore local**: exporta medicamentos + histórico para um `.json`, escolhendo o destino pelo seletor do próprio Android (inclui Google Drive, se instalado) — sem conta nem servidor.

## Decisões de modelagem importantes

- **`DoseRecord` só é gravado quando o usuário interage** com a dose (marca como tomada/pula). Doses futuras ou passadas nunca tocadas são tratadas como "pendentes virtuais" pelo `MedicationRepository`, cruzando `Medication.doseTimesOn(date)` com o histórico no momento da consulta. Isso evita ter que pré-gerar milhares de linhas para medicamentos de uso contínuo.
- **Cadência via `RecurrenceType`:** `Medication.isScheduledOn(date)` decide se um dia bate com o padrão (diário / dias da semana / a cada X dias); `Medication.scheduleTimes` deriva os horários do dia — fixos para os três primeiros tipos, gerados a partir de um horário-âncora + intervalo para "a cada X horas".
- **Alarmes por horário, não por medicamento:** o `AlarmScheduler` agenda um alarme do sistema por HORÁRIO distinto entre os medicamentos ativos (não um por medicamento). Quando dispara, o `AlarmReceiver` consulta o Room na hora pra descobrir quem está de fato agendado pra aquele instante hoje — é isso que permite agrupar tudo numa notificação só. `MedicationRepository.reconcileAlarms()` recalcula esse conjunto de horários (liga os novos, desliga os que ninguém mais usa) sempre que um medicamento é salvo/excluído, e também no boot.
- **Agendamento "auto-perpetuante":** como o AlarmManager não tem alarme exato recorrente, cada disparo reagenda o mesmo horário para o dia seguinte — parando sozinho quando nenhum medicamento mais precisa daquele horário (ou a data de término é ultrapassada). A soneca usa um disparo único separado, sem mexer nesse ciclo.
- **Indicador de adesão** (dias em vermelho na barra superior): calculado em memória cruzando os medicamentos ativos em cada dia passado com os `DoseRecord` de status `TAKEN` — sem job em background.
- **Backup é restauração, não mescla:** importar um `.json` apaga e recria as tabelas `medications`/`dose_records` dentro de uma transação (`AppDatabase.withTransaction`). A UI confirma isso com o usuário antes de deixar escolher o arquivo.

## Build

Pré-requisitos: Android Studio (Koala ou mais recente) ou `ANDROID_HOME` configurado + JDK 17.

```bash
./gradlew assembleDebug
./gradlew installDebug   # com um dispositivo/emulador conectado
```

> Este repositório já inclui o Gradle Wrapper (`./gradlew`). Se abrir no Android Studio, ele resolve o SDK automaticamente pela IDE.

## Testando os alarmes localmente

### 1. Conceder as permissões manualmente (uma vez)

No Android 13+, a notificação exige permissão em runtime (o app já pede isso na primeira abertura). No Android 12+, alarmes exatos exigem autorização explícita nas configurações do sistema — o app mostra um banner vermelho na Home enquanto isso não for feito, ou você pode conceder via adb:

```bash
adb shell dumpsys deviceidle whitelist +com.medicontrol.app
adb shell cmd notification allow_listener com.medicontrol.app
# Alarmes exatos (Android 12+): abra a tela pelo banner do app, ou:
adb shell am start -a android.settings.REQUEST_SCHEDULE_EXACT_ALARM -d package:com.medicontrol.app
```

### 2. Cadastrar um medicamento com horário próximo

Cadastre um remédio com um horário 1-2 minutos à frente do horário atual do emulador/aparelho — mais rápido do que esperar até o próximo dia.

### 3. Simular Doze Mode (o teste mais importante)

O Doze Mode só é simulável via `adb` (não existe toggle na UI do emulador):

```bash
adb shell dumpsys battery unplug
adb shell dumpsys deviceidle force-idle
# opcional: verificar o estado
adb shell dumpsys deviceidle get deep
```

Com o app em Doze e o alarme prestes a disparar, confirme que a notificação chega mesmo assim — é exatamente o que `setExactAndAllowWhileIdle()` garante. Para sair do Doze:

```bash
adb shell dumpsys deviceidle unforce
adb shell dumpsys battery reset
```

### 4. Testar o reagendamento após reboot

```bash
adb reboot
# depois que o aparelho voltar:
adb shell dumpsys alarm | grep -A 3 com.medicontrol.app
```

Você deve ver o alarme do `AlarmReceiver` listado novamente — prova de que o `BootReceiver` reagendou tudo a partir do Room.

### 5. Inspecionar os alarmes agendados a qualquer momento

```bash
adb shell dumpsys alarm | grep -B 2 -A 10 com.medicontrol.app
```

### 6. Adiantar o relógio do sistema (alternativa a esperar)

```bash
adb shell date $(date -d "+2 minutes" +%m%d%H%M%Y.%S)   # Linux/macOS com GNU date
```

Cuidado: isso também acelera outros apps/serviços do sistema; prefira cadastrar um horário próximo (passo 2) quando possível.

## Próximas features (planejadas, ainda não implementadas)

- **Relatório de adesão exportável** (PDF/CSV) para levar ao médico — os dados já existem em `DoseRecord`/`getMissedDays`, falta só a tela de exportação e o formato de saída.
- **Widget de tela inicial** ("próximas doses"), em Jetpack Glance. Mockup validado: https://claude.ai/artifact/BJ9kdCyu94qcXCBB6rtxdL (dois temas, dois tamanhos e o estado vazio).

## Sobre o Railway

O app é propositalmente **offline** (Room local, sem conta de usuário, sem sync) — não há necessidade de backend para o funcionamento descrito, incluindo o backup: ele usa o seletor de arquivos do próprio Android (Storage Access Framework), então salvar no Google Drive já funciona sem servidor, API key ou OAuth — quem resolve o "onde salvar" é o sistema.

Se no futuro você quiser **sincronização automática entre aparelhos** (sem o usuário exportar/importar manualmente), aí sim o Railway seria um bom lugar para hospedar uma API simples (ex.: FastAPI/Node + Postgres) por trás de uma conta de usuário — mas isso é uma funcionalidade bem maior (autenticação, resolução de conflito entre aparelhos), não o que foi pedido até aqui.
