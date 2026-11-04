# S9 · Tú vs IA: duelo con el modelo local

**Цель:** открыть «Tú vs IA» с Home, переключить модель (Ajustado/Base), ответить, увидеть вердикт и строку IA, перейти дальше и выйти.
**Предусловие:** чистое состояние. Сервис с моделью (`fine_tuning_prod/serve.py` + `adb reverse tcp:8090 tcp:8090`) может быть как запущен, так и нет: сценарий проверяет, что экран работает в обоих случаях — строка «IA:» есть и с ответом модели, и с «no disponible».

```steps
clear
launch
tap: Tú vs IA
expect: Tú vs IA · Pregunta 1 de 5
expect: Modelo:
expect: Ajustado
tap: Base
tap_option: 1
expect: Tú:
expect: IA:
expect: Siguiente
tap: Siguiente
expect: Pregunta 2 de 5
tap: Cerrar duelo
expect: Comenzar quiz
```

**Ожидание:** кнопка «Tú vs IA» в карточке «Historia y cultura» открывает дуэль из 5 вопросов; до ответа можно выбрать модель («Ajustado» — по умолчанию, «Base»); после ответа показаны вердикт «Tú: …» и строка «IA: …» (ответ с задержкой в мс, «pensando…» или «no disponible»); «Siguiente» ведёт ко второму вопросу со счётом «Tú N · IA M»; «Cerrar duelo» возвращает на Home. Статистика вопросов не меняется.
