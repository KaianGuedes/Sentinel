# Sentinel

**Sentinel** is a lightweight, powerful and secure authentication plugin for 1.20+ Minecraft servers. Built with simplicity and safety in mind, it ensures that players register and log in before interacting with the world, protecting your server from unauthorized access and potential exploits.

<p align="center">
  <a href="https://github.com/KaianGuedes/Sentinel/releases">
    <img src="https://img.shields.io/github/v/release/KaianGuedes/Sentinel?color=4183c4&label=Release&logo=github&style=flat-square" alt="GitHub Release">
  </a>
  <a href="https://modrinth.com/plugin/sentinel-auth">
    <img src="https://img.shields.io/badge/Modrinth-Download-1bd96a?style=flat-square&logo=modrinth&logoColor=white" alt="Modrinth">
  </a>
  <a href="https://www.spigotmc.org/resources/sentinel.125984/">
    <img src="https://img.shields.io/badge/Spigot-Download-orange?style=flat-square&logo=spigotmc&logoColor=white" alt="SpigotMC">
  </a> <br>
  <a href="https://www.codefactor.io/repository/github/kaianguedes/sentinel" style="display:inline-block; border-radius:8px; overflow:hidden; box-shadow: 0 0 5px rgba(0,0,0,0.2);">
    <img src="https://www.codefactor.io/repository/github/kaianguedes/sentinel/badge" alt="CodeFactor" style="display:block; border:none;">
  </a>
</p>

---

## 🔧 Installation
1. Download the latest plugin `.jar` from the [Releases](https://github.com/KaianGuedes/Sentinel/releases) tab.
2. Place the `.jar` file into your server's `plugins/` directory.
3. Start or restart your Minecraft server.
4. Sentinel will auto generate its storage file in `plugins/Sentinel/auth.db`.
5. Register with `/register <password> <password>` and log in with `/login <password>`.

## ⚙️ Configuration
All options live in `plugins/Sentinel/config.yml`. Keys missing from an older file fall back to the defaults below, so upgrading never breaks an existing config.

| Option | Default | Description |
|---|---|---|
| `language` | `en-US` | Server language, used for console logs. |
| `uuid-mode` | `adapt` | `adapt`, `online` or `offline`. See the note below. |
| `session.enabled` | `false` | Auto-login after a recent logout. See the risk note below. |
| `session.duration-minutes` | `5` | How long a session can be resumed. |
| `login.timeout-seconds` | `60` | Kick players that do not authenticate in time (`0` disables). |
| `login.max-attempts-per-player` | `5` | Failed attempts per account before kick and lockout (`0` disables). |
| `login.max-attempts-per-ip` | `10` | Failed attempts per IP address before kick and lockout (`0` disables). |
| `login.lockout-seconds` | `300` | Lockout length. Locked players or IPs are refused at join. |
| `password.min-length` | `8` | Minimum password length. |
| `password.max-length` | `64` | Maximum password length. Passwords over 72 bytes are always refused (bcrypt limit). |
| `password.bcrypt-cost` | `12` | bcrypt work factor (4 to 20). Existing hashes keep working at any cost. |
| `commands.aliases.login` | `[l]` | Extra names for `/login`. |
| `commands.aliases.register` | `[]` | Extra names for `/register`. The old default `r` was removed because it collides with EssentialsX `/r`. |
| `commands.aliases.changepassword` | `[]` | Extra names for `/changepassword`. |
| `floodgate.skip-auth` | `false` | Let Floodgate-authenticated Bedrock players skip `/login`. Needs Floodgate installed. |

Language files in `plugins/Sentinel/lang` are created on first start. Missing keys fall back to the bundled text, so new messages appear after an update even if you edited a file.

### UUID mode
`adapt` follows the server's `online-mode`. If you ever change `online-mode`, every account is stored under a different UUID and all players appear unregistered (their old accounts are orphaned). Pick `online` or `offline` explicitly on any server that might change mode, or sits behind a proxy.

### Sessions (auto-login) risk
Sessions are off by default. When enabled, a player who logged out or disconnected can rejoin without a password if the same account connects from the same IP address with the same client brand within `session.duration-minutes`. Each session is single use, tied to a random server-side token, and removed on `/logout`, `/unregister` and password changes. Anyone who shares the player's IP (same household, school or VPN exit) can take over the account during that window, so only enable it if you accept that risk. Bedrock players are never auto-logged in.

### Names
Accounts are matched without regard to case: once `Steve` is registered, `steve` cannot register. Accounts created by older versions are linked to their name the first time they log in.

## 🔒 Password privacy and command loggers
`/login`, `/register` and `/changepassword` (and their configured aliases) are processed by Sentinel in `PlayerCommandPreprocessEvent` at `LOWEST` priority and then cancelled, with the message replaced by a placeholder. Plugins that log commands at a normal priority or later, such as CoreProtect or EssentialsX commandspy, never see the password. A console log filter that hides the vanilla "issued server command" line is built from the real command names and aliases and is removed when Sentinel is disabled. A plugin that listens at `LOWEST` and loads before Sentinel can still see the raw command, so check your plugin list if you rely on this.

## 🧱 Protection while logged out
Until a player authenticates they are held in spectator mode with blindness and cannot move, chat, run other commands, break or place blocks, interact, drop or pick up items, use inventories, deal or take damage, or be targeted. Their original game mode, OP status, effects and location are saved when they join and restored on login. Unregistering an account clears this data, so a new owner of the name does not inherit OP.

Database errors fail closed: if the database cannot be read the player is kicked with a message instead of being treated as unregistered. Password hashing and database access run off the main thread. Sentinel uses the Bukkit scheduler and does not support Folia. `/reload` is safe: online players are re-restricted and must log in again.

## 📢 Planned  / Coming Soon
- Currently no planned features, but open to suggestions and pull requests!

## 🤝 Need Help or Want to Contribute?
Found a bug? Have a suggestion? Want to help improve it?
You can:
- Open [issues](https://github.com/KaianGuedes/Sentinel/issues) for bugs, ideas, or feature requests.
- Submit [pull requests](https://github.com/KaianGuedes/Sentinel/pulls) to improve the code, docs, or features.
- Or start a [discussion](https://github.com/KaianGuedes/Sentinel/discussions) in the GitHub repository.

> ⭐ Star the repo to support the project!

## 🛠️ Developer Info
Built with:
- Java 17 (builds with JDK 17 to 25, tested on Paper 26.3)
- Gradle
- Paper API

## 📃 License
This project is open source under the [GPL-3.0](LICENSE).

