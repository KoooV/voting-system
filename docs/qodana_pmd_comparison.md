# Сравнительный отчёт: Qodana vs PMD (проект: voting-system)

Дата: 2025-12-01

Краткое назначение

- Этот файл содержит сравнительный анализ результатов статического анализа кода между Qodana и PMD для репозитория `voting-system`.
- Примечание: в репозитории найден PMD-отчёт (`build/reports/pmd/main.html` и `reports/analysis/initial/pmd/main.xml`). Отчёт Qodana был сгенерирован локально и добавлен в `reports/qodana/qodana.sarif.json`, далее распарсен для сравнения.

План сравнения

1. Перечислить найденные отчёты и их местоположение.
2. Распарсить PMD-отчёт и собрать все нарушения (файл/строка/правило/описание).
3. Распарсить Qodana SARIF (Code Climate SARIF) и собрать все обнаруженные проблемы.
4. Нормализовать пути и сопоставить дефекты (точно по file+line+rule и по location).
5. Классифицировать (real/style/false-positive) и подготовить рекомендации.

Найденные отчёты

- PMD:
  - `reports/analysis/initial/pmd/main.xml` (распарсено)
  - `reports/analysis/with-errors/pmd/main.xml` (распарсено)
  - `config/pmd/pmd-ruleset.xml` (конфигурация правил)
- Qodana:
  - SARIF: `/tmp/qodana_output/qodana.sarif.json` (сгенерирован локально) — распарсен и данные записаны в `reports/analysis/qodana_pmd_issues.csv`.

Результаты парсинга и сопоставления (краткая статистика)

- Всего записей PMD (всех нарушений, обе папки): 102
- Всего записей Qodana (SARIF): 9
- Уникальных мест (file+line) после объединения: 49
  - Найдены обоими инструментами (Both): 0
  - Только PMD: 45
  - Только Qodana: 4

Примечание: отсутствие совпадений "Both" означает, что Qodana в этом прогоне в основном сообщала о проблемах, которые не накладываются на PMD-нарушения (часто — в сгенерированных/внешних файлах или в артефактах), и наоборот — PMD дал множество правил стиля в исходниках, которые Qodana не дублирует.

Файл CSV с полным списком (tool-агрегированное):

- `reports/analysis/qodana_pmd_issues.csv` — содержит строки с колонками: file, line, pmd_rule, pmd_msg, q_rule, q_msg, found_by, classification

Примеры уникальных Qodana-нарушений (Qodana-only)

- `reports/analysis/with-errors/jacoco/test/html/jacoco-resources/prettify.js:1273` — RegExpUnexpectedAnchor ("Anchor '^' in unexpected position") — в сгенерированных ресурсах отчёта Jacoco. Классификация: review (в большинстве случаев — ложное/артефактное, т.к. это сгенерированные файлы).
- `target/cucumber-report.html:47` — RegExpSingleCharAlternation — в результирующем HTML отчёта Cucumber (сгенерированное/артефактное).
- `build.gradle.kts:40` — VulnerableLibrariesLocal (Mend): обнаружена транзитивная уязвимая зависимость `org.apache.commons:commons-lang3:3.17.0` (CVE-2025-48924). Классификация: high/important — требует внимания (менеджер зависимостей).
- `reports/analysis/initial/jacoco/test/html/jacoco-resources/prettify.js:1273` — аналогичная RegExpUnexpectedAnchor в другом месте.

Примеры уникальных PMD-нарушений (PMD-only)

- Большинство — правила стиля и небольшие рекомендации (низкий приоритет):
  - `MethodArgumentCouldBeFinal` (25 записей) — параметрам можно поставить `final`.
  - `ShortVariable` (8 записей) — короткие имена (`id`, `ex`) в контроллерах.
  - `LocalVariableCouldBeFinal` (6 записей).
- Некоторые потенциально полезные рекомендации (средний приоритет):
  - `MissingSerialVersionUID` (4 записи) — у исключений нет `serialVersionUID`.
  - `UseUtilityClass` для `VotingSystemApplication` — ложное срабатывание (это Spring Boot application, не utility class).
  - `CollapsibleIfStatements`, `AtLeastOneConstructor` и т.п. — рефакторинговые подсказки.

Анализ ложных срабатываний и контекст

- Qodana часто сообщил о проблемах в сгенерированных/временных файлах или сторонних артефактах (`reports/...`, `target/...`). Это ожидаемо при конфигурации, которая не исключает такие директории. Рекомендация: в настройке Qodana (и в CI) исключать `reports/`, `target/`, `build/` и сгенерированные ресурсы, если хотите анализировать только исходники `src/`.

- PMD выдал много правил код-стиля, которые в реальности не являются багами, но могли быть включены по умолчанию в `config/pmd/pmd-ruleset.xml`. Рекомендация: откорректировать ruleset, отключив или смягчив правила, которые вызывают шум (см. раздел рекомендаций).

Совпадения между инструментами

- В прогоне, который мы распарсили, пересечений (одинаковые file+line) между Qodana и PMD не обнаружено. Причины:
  - Qodana в этом запуске фокусировался на других классах правил (регулярные выражения в ресурсах, проверка зависимостей), а PMD — на стилевых правилах в исходниках Java.
  - Инструменты имеют разные цель и покрытие: PMD — правило-ориентированный анализ исходного Java-кода; Qodana включает множество плагинов/анализаторов (включая SAST, Dependency checks, RegExp checks в артефактах), поэтому пересечения не обязаны появляться.

Рекомендации (конкретные действия)

1) Игнорировать/исключать сгенерированные файлы из Qodana
   - В `qodana.yaml` (или при запуске `scan`) добавить исключения для `reports/**`, `target/**`, `build/**`, `**/prettify.js` и т.п.
   - Это сократит ложные/артефактные уведомления и позволит фокусироваться на исходниках.

2) Работа с уязвимой зависимостью (build.gradle.kts)
   - Проверить транзитивную зависимость `org.apache.commons:commons-lang3:3.17.0` и обновить до безопасной версии (рекомендуется проверить CVE-данные и матрицу совместимости).
   - Команда для локальной проверки зависимостей (Gradle):

```bash
./gradlew dependencyInsight --configuration runtimeClasspath --dependency org.apache.commons:commons-lang3
```

3) Подавление шумных правил PMD
   - В `config/pmd/pmd-ruleset.xml` можно отключить ряд правил, если команда не нацелена на строгую code-style политику, например:

```xml
<!-- Пример предложений (адаптируйте под вашу версию ruleset) -->
<!-- Отключить требование final-параметров -->
<rule ref="category/java/codestyle.xml/MethodArgumentCouldBeFinal" enabled="false"/>
<rule ref="category/java/codestyle.xml/LocalVariableCouldBeFinal" enabled="false"/>

<!-- Отключить строгие правила по коротким/длинным именам в API -->
<rule ref="category/java/codestyle.xml/ShortVariable">
  <exclude-pattern>src/main/java/com/kov/votingsystem/api/.*</exclude-pattern>
</rule>
```

   - Либо подавлять конкретные предупреждения в исходниках с помощью `@SuppressWarnings("PMD.RuleName")`.

4) CI-пайплайн: интеграция и полиси
   - Запускать оба инструмента: PMD (Gradle task) и Qodana (Docker/CLI) в CI.
   - Сохранять артефакты: `pmd/main.xml`, `qodana.sarif.json`.
   - Ввести правило: "fail build, если появились новые критические/уязвимости (по Qodana/Mend)"; стилистические ошибки — показывать, но не блокировать (или блокировать только по PR-policy).
   - Использовать SARIF-baseline для Qodana (флаг `--baseline`) чтобы не шуметь по старым проблемам.

5) Автоматизация: парсинг и отчетность
   - CSV с результатом сопоставления `reports/analysis/qodana_pmd_issues.csv` уже создан — его можно прикрепить к задаче/issue-tracker для triage.
   - Добавить скрипт в `scripts/` для автоматического создания такого CSV при каждом CI-run.

Пара примечаний по классификации

- Большая часть PMD-нарушений в проекте — код-стиль: низкий приоритет, можно подавлять централизованно.
- Qodana обнаружил одну значимую проблему в зависимостях (VulnerableLibrariesLocal) — это самое важное из найденного и требует обновления зависимостей или оценки воздействия.
- Несколько Qodana-нарушений — в сгенерированных отчетах/HTML/JS: рекомендуется исключить эти директории.

Дальшие шаги (что я могу сделать автоматически дальше)

- Пройти по CSV и для каждого `classification` = `potential` или `likely_real` открыть соответствующий исходник и приложить 3–5 строк контекста (я могу дополнить CSV/MD примерами кода).
- Сгенерировать `patches/pmd_ruleset_suggestions.diff` с конкретными изменениями для `config/pmd/pmd-ruleset.xml` (предложенные включения/исключения).
- Подготовить минимальный CI-скрипт (GitHub Actions / GitLab CI snippet) для запуска Qodana и PMD и публикации SARIF/PMD XML.

Где лежит CSV

- `reports/analysis/qodana_pmd_issues.csv` — откройте его для детального списка и фильтрации. Этот CSV — основной артефакт для triage.

Заключение / краткая сводка

- PMD: много предупреждений код-стиля (низкий приоритет). Рекомендую отфильтровать шум в `config/pmd/pmd-ruleset.xml`.
- Qodana: выявила несколько артефактных проблем (в отчётах) и одну важную уязвимость в зависимостях; это — приоритет номер один для исправления.
- Пересечение между инструментами в текущем прогоне отсутствует — инструменты дополняют друг друга.

---

Если хотите, я продолжу автоматически:
- A) добавлю 3–5 строк контекста к каждому `classification` = `potential|likely_real` в CSV/MD, или
- B) подготовлю `patches/pmd_ruleset_suggestions.diff` и предложу точечные изменения в `config/pmd/pmd-ruleset.xml`, или
- C) подготовлю CI-snippet для GitHub Actions, который запускает Qodana и PMD и сохраняет артефакты.

Напишите, что предпочитаете (A/B/C) — я выполню выбранное действие и обновлю репозиторий.
