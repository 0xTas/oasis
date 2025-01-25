# Oasis


Oasis is a plugin for the Minecraft 1.12.2 utility mod [Lambda](https://github.com/lambda-client/lambda).
<br>
It includes a collection of custom modules originally designed for use on the anarchy server 2B2T.
<br><br>
**[2b is on Minecraft version 1.19](https://2b2t.org/update/) as of August 14th 2023, meaning Lambda, and by extension Oasis, [are no longer useful on the server](https://github.com/0xTas/oasis#whats-next).**<br><br>
**If you are looking for a 1.20+ replacement for Oasis, consider my [Meteor Client](https://meteorclient.com) addon [Stardust](https://github.com/0xTas/stardust).**

### Modules
| Name           | Description                                                                          | Usage                                                                                                                                                             | Why                                                                                                                                                                                                                                                                                                              |
|----------------|--------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| EntityJesus    | Allows you to ride entities (mainly horses) over water.                              | Found in the **Movement** category of the [ClickGui](https://github.com/lambda-client/lambda#faq). Bind to the same key as EntitySpeed and away you go!           | Lambda 3.3 introduced a Jesus rewrite which removed that ability.                                                                                                                                                                                                                                                |
| FarmAura       | Automatically preforms farming tasks for you.                                        | Found in the **Player** category of the ClickGui. See configuration options.                                                                                      | There were no good free options for an AutoFarm that I knew of, so I made one.                                                                                                                                                                                                                                   |
| OldSigns       | Highlights signs placed prior to Minecraft version 1.8 (before 2015).                | Found in the **Render** category of the ClickGui. (**not compatible with optifine shaders**)                                                                      | I've seen this module used by Paulsteve007 but I couldn't find it anywhere, so I figured out the trick and recreated it myself.                                                                                                                                                                                  |
| TunnelESP      | Highlights narrow tunnels in the world around you.                                   | Found in the **Render** category of the ClickGui. (**not compatible with optifine shaders**)                                                                      | Lambda was sorely missing this one and I ain't gonna install a separate client for one module. My first try was unusably slow. Optimizing it was a fun challenge :^)                                                                                                                                             |
| ChatSigns      | Prints nearby signs into your chat (locally).                                        | Found in the **Chat** category of the ClickGui. Optionally displays sign coords and old status.                                                                   | I like reading signs on the highway without physically stopping for each one.                                                                                                                                                                                                                                    |
| SignatureSign  | Automatically fills signs with custom text.                                          | Found in the **Misc** category of the ClickGui. Includes read-from-file mode and template mode with many options.                                                 | Again, no free option that I know of. Use it to sneak unicode characters onto signs or just out of pure laziness :)                                                                                                                                                                                              |
| AutoDoors      | Automatically closes doors behind you, like a civilized steve.                       | Found in the **Player** category of the ClickGui. Can also open doors in front of you.                                                                            | I noticed that literally everyone is too lazy to close doors in this game so I made a module that does the larping for you. Also there's a secret easter-egg feature in this, if you can read the code it's ez but if you find it lmk :)                                                                         |
| BannerInfo     | Right-click banners to display their pattern and color data.                         | Found in the **Misc** category of the ClickGui. Useful when making copies.                                                                                        | Vanilla shows you everything but the base info, which leaves you guessing with many designs. Lambda's BlockData module works for banners but spits the metadata out as basically gibberish. Mine is nicely formatted and plain english.                                                                          |
| NotepadHud     | Write custom text to the HUD.                                                        | Found in the **Misc** category of the HudEditor.                                                                                                                  | You can use it to write color-coded sticky notes to your hud, or just for fun.                                                                                                                                                                                                                                   |
| SegFault       | Crashes your game?                                                                   | Command only. Type "**;segfault**", "**;crash**", or "**;hcf**" to crash your game.                                                                               | Reference to a joke from the suggestions channel in [Lambda's Discord](https://discord.gg/QjfBxJzE5x). I thought it was funny, so I implemented it :]                                                                                                                                                            |
| LastSeen       | Checks the last-seen status of a 2b2t player. (credit [2b2t.dev](https://2b2t.dev/)) | Command only. Type "**;ls 0xTas**", or whoever else you want to check.                                                                                            | The bot has been muted for a long time and somebody brought the api to my attention. **EDIT:** as of [7/12/2023](https://github.com/lambda-client/lambda/pull/524), the nightly version of Lambda now includes built-in 2b2t api commands. Oasis versions before 1.6.1 are incompatible, new versions use `;ls`. |
| UpdateNotifier | Notifies you in chat (once per session) when an update is available.                 | Click the link in chat to be taken to the latest release on Github. You can permanently disable this by clicking on it in the **Client** section of the ClickGUI. | It's nice to know when new features or improvements are available without needing to stalk the repo.                                                                                                                                                                                                             |
---
### Installation & Updating
1. Firstly, ensure that Minecraft 1.12.2, Forge, and [Lambda](https://github.com/lambda-client/lambda) are installed.
2. [Download the plugin](https://github.com/0xTas/oasis/releases/latest/) or [build it from source](https://github.com/0xTas/oasis#contributing).
3. *Before starting the game*, place the JAR file in your `.minecraft/lambda/plugins` folder.
---
### Building & Contributing
1. Clone the repository: `git clone https://github.com/0xTas/oasis`.
2. Follow the [IDE Setup Instructions](https://github.com/lambda-client/ExamplePlugin#setup-ide) from the Lambda Example-Plugin repo.
3. Ensure that `Oasis > Tasks > build > build` & `Oasis > Tasks > shadow > shadowJar` run successfully.
4. When building the final JAR, use the shadowJar task.
5. You're all set!
---
### What's Next?

Now that 2b2t has updated to 1.19, Lambda & Oasis are no longer useful or relevant on the server.<br>
If Lambda ever does update to support 1.20+, I may update the plugin (where possible) to match.<br>
<br><br>
> ### Disclaimer
> This plugin modifies your in-game experience in a way that may be considered unfair in a multiplayer environment.<br>
> Do not use this plugin on servers without permission from the admins.<br>
> By using this plugin, you agree to take responsibility for any bans or disciplinary actions taken against you in response.
