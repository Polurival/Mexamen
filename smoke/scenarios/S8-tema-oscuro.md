# S8 · Tema: Sistema → Claro → Oscuro con guardado

**Цель:** на Home иконка темы переключает режим по циклу Sistema → Claro → Oscuro → Sistema, выбор переживает перезапуск приложения (#23).
**Предусловие:** чистое состояние (после `clear` тема — «Sistema»).

```steps
clear
launch
expect: Comenzar quiz
expect: Tema: Sistema
tap: Tema: Sistema
expect: Tema: Claro
tap: Tema: Claro
expect: Tema: Oscuro
launch
expect: Comenzar quiz
expect: Tema: Oscuro
tap: Estadísticas
expect: Preguntas totales
back
expect: Tema: Oscuro
tap: Tema: Oscuro
expect: Tema: Sistema
launch
expect: Tema: Sistema
```

**Ожидание:** content-desc иконки в правом верхнем углу Home — «Tema: <режим>»; каждое нажатие сразу меняет режим и цвета всего приложения (Claro — светлая схема, Oscuro — тёмная независимо от системной настройки); после `launch` (перезапуск процесса) режим тот же, что был выбран; другие экраны (Estadísticas) открываются в выбранной теме. Цвета смотреть по скриншотам шагов — раннер их не сравнивает.
