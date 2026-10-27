# S5 · Leyendas: список и детальный экран

**Цель:** открыть список легенд, зайти в одну и вернуться.
**Предусловие:** чистое состояние.

```steps
clear
launch
tap: Leyendas
expect: Leyendas Mexicanas
expect_absent: Back
tap: El cenote Zací
expect_absent: Leyendas Mexicanas
expect: Reproducir audio
expect_absent: Back
back
expect: Leyendas Mexicanas
tap: El cenote Zací
expect_absent: Leyendas Mexicanas
tap: Volver
expect: Leyendas Mexicanas
back
expect: Comenzar quiz
```

**Ожидание:** список легенд открывается, легенда показывается отдельным экраном; кнопки-иконки подписаны по-испански (content-desc «Volver», «Reproducir audio», без английского «Back»); и системная «назад», и кнопка «Volver» в приложении ведут обратно в список, затем «назад» — на главную.
