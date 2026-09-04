<h1 align="center">
    <img src="https://github.com/CptbeffHeart/DeepStorage/assets/51067790/56b79f88-439c-411e-b484-5ccc355ce0ef">
</h1>

# Description
[Nova](https://github.com/xenondevs/Nova) addon which adds a block to store bulk items.

This is a fork of [CptbeffHeart/DeepStorage](https://github.com/CptbeffHeart/DeepStorage), ported from
Nova 0.16 to **Nova 0.22.3 (Minecraft 1.21.11)**. Gameplay is unchanged; the code was rewritten against
the current Nova and InvUI 2 APIs, and the GUI textures were redrawn in the style of Nova's own buttons
and placeholders.

| Addon version | Nova version | Minecraft |
|---------------|--------------|-----------|
| 3.x           | 0.22.3       | 1.21.11   |
| 2.0.0 (upstream) | 0.16      | 1.20.4    |

# How to use

## Obtain
All the items can be taken from the inventory `/nova items` on the `Deep_Storage` tab
<br>
You can use the command `/nova give <player> deep_storage:<itemName>`

## Setup
Add storage cells in the storage cell inventory to increase storage capacity. Drop items into the
input slot, or onto the item list, to store them; click a stored item to take a stack out
(right click takes one, shift click puts it straight into your inventory).

## Sorting
Sort the item list by amount or by name with the sort button.

## Security
A security card can be set up by shift right-clicking with it in hand.
<br>
The security card can be cleared by shift right-clicking with it in hand.
<br>
The deep storage security can be activated by clicking on the lock on the top right of the security inventory.

When activated, the security prevents other players from opening the deep storage unit menu or breaking it.
<br>
When the security is activated, the players that can access the storage are:
<br>- The block's owner
<br>- Op players
<br>- Players with the permission `deep_storage.security.bypass`
<br>- Players with a security card in the security menu

## Side config
Side config can be set to input or output as on the other configurable blocks. The front face is
never connected.

# Install
Add [Nova 0.22.3](https://github.com/xenondevs/Nova) to your plugin folder, then restart the server.<br>
Put this addon's jar in the `plugins` folder next to Nova (since Nova 0.17, addons are Paper plugins).
>[!Tip]
> You can follow the [official nova setup guide](https://xenondevs.xyz/docs/nova/admin/setup/).

# Build
```
./gradlew addonJar
```
The jar lands in `build/libs/`. Pass `-PoutDir=<dir>` to write it somewhere else.

# Credits
Original addon by [CptBeffHeart](https://github.com/CptbeffHeart)
<br>
Textures by Lil Essence (Discord: lilessencelerageulemalandrin)
<br>
Inspired by [DeepStoragePlus](https://github.com/christopherwalkerml/DeepStoragePlus) and [Applied Energistics](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2)
