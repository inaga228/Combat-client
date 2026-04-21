# Combat Client

A feature-rich Minecraft Forge 1.16.5 client mod.

## Modules

| Category | Modules |
|---|---|
| Combat | KillAura, Reach, AntiKnockback, AutoCrystal, Velocity |
| Renderer | ESP, Trajectories, Chams, FullBright, NoWeather |
| HUD | HUD, ArmorHUD, PotionHUD, Notifications |
| Player | NoFall, FastPlace, AutoEat, Sprint, AntiVoid |

## How to use

- **Right Shift** — open ClickGUI
- **Left-click** module — toggle on/off
- **Right-click** module — open settings (sliders, toggles, enums)
- **Bind row** in settings — click then press a key to bind

## Releasing

1. Update `CHANGELOG.md` with new version block `## [x.x.x]`
2. Push to main
3. `git tag vx.x.x && git push origin vx.x.x` — triggers GitHub Release automatically
