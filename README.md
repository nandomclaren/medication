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
│   ├── model/           # Enums e DoseUiModel
│   └── repository/      # MedicationRepository (une Room + AlarmManager)
├── ui/
│   ├── theme/            # Color/Theme/Type — Material You + dark/light
│   ├── components/       # MedicationIconView (Canvas), DaySelector, Icon/ColorPickerRow
│   ├── home/              # Tela principal
│   ├── addedit/           # Cadastro/edição de medicamento
│   └── navigation/        # NavHost
└── util/                    # Formatação de datas/horas
```

## Decisões de modelagem importantes

- **`DoseRecord` só é gravado quando o usuário interage** com a dose (marca como tomada/pula). Doses futuras ou passadas nunca tocadas são tratadas como "pendentes virtuais" pelo `MedicationRepository`, cruzando `Medication.times` com o histórico no momento da consulta. Isso evita ter que pré-gerar milhares de linhas para medicamentos de uso contínuo.
- **Agendamento "auto-perpetuante":** em vez de agendar todas as doses futuras (inviável para uso contínuo), o `AlarmScheduler` agenda só a *próxima* ocorrência de cada horário. Quando o `AlarmReceiver` dispara, ele mostra a notificação e reagenda o mesmo horário para o dia seguinte — parando sozinho quando a data de término é ultrapassada.
- **Indicador de adesão** (dias em vermelho na barra superior): calculado em memória cruzando os medicamentos ativos em cada dia passado com os `DoseRecord` de status `TAKEN` — sem job em background.

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

## Sobre o Railway

O app é propositalmente **offline** (Room local, sem conta de usuário, sem sync) — não há necessidade de backend para o funcionamento descrito. O Railway não entra no fluxo atual.

Se no futuro você quiser **backup/sincronização entre aparelhos**, o Railway seria um bom lugar para hospedar uma API simples (ex.: FastAPI/Node + Postgres) que exporta/importa os dados do Room via JSON — mas isso é uma funcionalidade nova, não algo necessário para o app funcionar como especificado aqui.
