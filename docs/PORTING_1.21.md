# EvaulxMC — 1.8.8 → Paper 1.21 Port Status

Branch: `port/modern-1.21` (the original working 1.8.8 build is preserved on `main`).

## ✅ Done — compiles & packages on Paper 1.21 (JDK 21)
- Build retargeted to `io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT`, Java release 21.
- Dropped the unused Lombok dependency; bumped `maven-shade-plugin` to 3.6.0 (Java-21 capable).
- All legacy `Material` names mapped to modern (1.13+) equivalents, in code **and** `config.yml`
  (e.g. `WATCH`→`CLOCK`, `EYE_OF_ENDER`→`ENDER_EYE`, `SKULL_ITEM`→`PLAYER_HEAD`,
  `STAINED_GLASS_PANE`→`*_STAINED_GLASS_PANE`, `STAINED_CLAY`→`*_TERRACOTTA`, `SIGN`→`OAK_SIGN`,
  `WORKBENCH`→`CRAFTING_TABLE`, `DIODE_BLOCK_*`→`REPEATER`, `REDSTONE_COMPARATOR_*`→`COMPARATOR`,
  `BOOK_AND_QUILL`→`WRITABLE_BOOK`, `EMPTY_MAP`→`MAP`, etc.).
- All legacy `Enchantment` names mapped to modern (`DAMAGE_ALL`→`SHARPNESS`, `DURABILITY`→`UNBREAKING`,
  `ARROW_DAMAGE`→`POWER`, …) in `EnchantCommand`.
- `Inventory#getTitle()` calls switched to `InventoryView#getTitle()`.
- Action bars rewritten to Paper's **Adventure** API (`player.sendActionBar(Component)`), replacing
  the dead 1.8 NMS reflection.
- Login ban/IP-ban checks moved to `AsyncPlayerPreLoginEvent`.
- `plugin.yml` now declares `api-version: '1.21'`.
- `mvn clean package` → BUILD SUCCESS, 8/8 tests pass, shaded jar produced.

## ⚠️ MUST be tested on a live 1.21 server before sale
A clean compile does **not** prove runtime behaviour. The following need a real Paper 1.21 server:
- **GUIs** — confirm every menu opens, items render, and clicks work (staff panel, tags, grant,
  punish, disguise menus, lobby protection, dashboard).
- **Nametags / scoreboard teams / tab list** — verify prefixes/suffixes and the team logic on 1.21.
- **Chat, punishments, ranks, grants, staff tools** — smoke-test end to end.

## ❌ Known NOT working on 1.21 yet — needs real work
- **Disguises (skin/profile packets).** `DisguiseManager` (~2300 lines) still uses 1.8
  `net.minecraft.server.<version>.*` reflection, which does not exist on 1.21. The reflective calls
  are all try/catch-guarded and fall back to Bukkit `hidePlayer`/`showPlayer`, so display-name/nick
  changes degrade gracefully, but **skin and GameProfile refresh will not work**. This needs a proper
  1.21 rewrite — recommended via ProtocolLib 1.21 packet wrappers or a maintained disguise/skin
  library — followed by live testing. This is the single biggest remaining task.
- **Colored GUI items via data values.** Some items were built with the deprecated
  `new ItemStack(Material, amount, dataByte)` constructor (e.g. rank-coloured terracotta/glass). The
  data value is ignored on 1.13+, so those render as the base colour. Map rank colour → a specific
  coloured `Material` to restore the look.

## Toolchain notes
- Build requires **JDK 21** and (first build only) network to fetch Paper deps. This machine needs
  the Maven Wagon SSL workaround due to a local certificate-trust issue:
  ```
  mvnw.cmd -Dmaven.resolver.transport=wagon -Dmaven.wagon.http.ssl.insecure=true \
           -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
           clean package
  ```
  After deps are cached, `mvnw.cmd -o clean package` works offline.
- The 1.8.8 pom is saved as `pom-1.8.8-backup.xml`; `main` has the full 1.8.8 source.
