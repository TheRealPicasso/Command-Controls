# Command Control

A Fabric mod for Minecraft 1.20.1+ that gives server owners complete control over which commands players can see and use.

## Features

- **Hide Commands from Tab-Complete**: Unauthorized commands don't appear in suggestions
- **Block Command Execution**: Prevent players from running commands they shouldn't have access to
- **Client-Side Filtering**: Also hides client-side mod commands (like Xaero's Minimap, Litematica, etc.)
- **Rank-Based Permissions**: Configure different commands for different player ranks
- **LuckPerms Integration**: Automatically detects player ranks from LuckPerms
- **Easy Configuration**: Simple JSON config file

## Installation

### Server
1. Install Fabric Loader and Fabric API
2. Place `commandcontrol-x.x.x.jar` in your `mods` folder
3. (Optional) Install LuckPerms for rank-based permissions

### Client
1. Install Fabric Loader and Fabric API
2. Place `commandcontrol-x.x.x.jar` in your `mods` folder

**Note**: The mod works on server-only, but for client-side command filtering (hiding commands from mods like Xaero's Minimap), players need to have it installed too.

## Configuration

Configuration is located at `config/commandcontrol/commands.json`

```json
{
  "_comment": "Command Control Configuration",
  "rank_hierarchy": ["default", "premium", "vip", "vip+", "moderator", "admin", "owner"],
  "bypass_commands": ["help", "list"],
  "commands": {
    "all_ranks": ["help", "list", "spawn", "home", "balance", "pay"],
    "vip": ["nick", "hat", "craft"],
    "moderator": ["kick", "mute", "vanish", "tp"],
    "admin": ["ban", "give", "gamemode", "fly"],
    "owner": ["stop", "reload", "op", "luckperms"]
  }
}
```

### Configuration Options

- **rank_hierarchy**: Order of ranks from lowest to highest. Higher ranks inherit commands from lower ranks.
- **bypass_commands**: Commands that are always visible and usable by everyone
- **commands.all_ranks**: Commands available to all players
- **commands.[rank]**: Commands available to players with that rank (and higher)

## How It Works

1. **Server-Side**: The mod intercepts command tree packets and filters out unauthorized commands before sending to clients
2. **Client-Side**: When connected to a CommandControl-enabled server, the client receives a list of allowed commands and filters suggestions locally (including from client-side mods)
3. **Execution Blocking**: Even if a player somehow sends a command they shouldn't have access to, the server blocks execution

## LuckPerms Integration

The mod automatically detects player ranks using LuckPerms groups. Make sure your LuckPerms groups match the rank names in the config.

If LuckPerms is not installed, the mod falls back to Minecraft's OP levels:
- OP Level 4 = owner (full access)
- OP Level 3 = admin
- OP Level 2 = moderator
- Everyone else = default

## Compatibility

- Minecraft 1.20.1
- Fabric Loader 0.14.21+
- Fabric API
- Works with LuckPerms (optional)
- Compatible with most other mods

## License

MIT License - See [LICENSE](LICENSE) for details.

## Links

- [Modrinth](https://modrinth.com/mod/commandcontrol)
- [GitHub](https://github.com/VincentPorath/CommandControl)
- [Issues](https://github.com/VincentPorath/CommandControl/issues)
