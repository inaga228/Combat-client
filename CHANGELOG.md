# Changelog

## [1.2.0]
### Added
- **Criticals** — порт с Meteor Client. Режимы: Packet, Jump, MiniJump, None. Интегрирован в KillAura — автоматически бьёт критами.
- **AutoTotem** — порт с Meteor Client. Режимы: Smart (по условиям HP/Elytra/Fall) и Strict (всегда). Настройки: Health, Delay, Elytra, Fall.

### Changed
- **ClickGUI** полностью переписан:
  - Анимация появления: панели плавно «падают» сверху
  - Анимированный collapse/expand (интерполяция высоты)
  - GL Scissor — контент обрезается по границе панели
  - Скролл колёсиком в панелях и в окне настроек
  - Полоска скролла с ручкой
  - Окно настроек не выходит за экран

## [1.1.0]
### Changed
- Оставлены только KillAura (улучшенная) и FastPlace
- KillAura переписана: плавное наведение, GCD, рандомизированный CPS, raytrace, автосвап

## [1.0.0]
### Added
- Initial release
