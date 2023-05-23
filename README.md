# Oasis


Oasis is a plugin for the Minecraft 1.12.2 utility mod [Lambda](https://github.com/lambda-client/lambda).
<br>
It includes a collection of custom modules designed for use on the anarchy server 2B2T.

### Modules
| Name          | Description                                             | Usage                                                                                                                                               | Why                                                                                                                                                   |
|---------------|---------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| EntityJesus   | Allows you to ride entities (mainly horses) over water. | Found in the movement category of the [ClickGui](https://github.com/lambda-client/lambda#faq). Bind to the same key as EntitySpeed and away you go! | Lambda 3.3 introduced a Jesus rewrite which removed that ability.                                                                                     |
| FarmAura      | Automatically preforms farming tasks for you.           | Found in the player category of the ClickGui. See configuration options.                                                                            | There were no good free options for an AutoFarm that I knew of, so I made one.                                                                        |
| ChatSigns     | Prints nearby signs into your chat (locally).           | Found in the chat category of the ClickGui. Optionally displays sign coords.                                                                        | I like reading signs on the highway without physically stopping for each one.                                                                         |
| SignatureSign | Automatically fills signs with custom text.             | Found in the misc category of the ClickGui. Includes read-from-file mode and template mode with many options.                                       | Again, no free option that I know of. Use it to sneak unicode characters onto signs or just out of pure laziness :)                                   |
| NotepadHud    | Write custom text to the HUD.                           | Found in the misc category of the HudEditor.                                                                                                        | You can use it to write color-coded sticky notes to your hud, or just for fun.                                                                        |
| SegFault      | Crashes your game?                                      | Command only. Type ;segfault, ;crash, or ;hcf to crash your game.                                                                                   | Reference to a joke from the suggestions channel in [Lambda's Discord](https://discord.gg/QjfBxJzE5x). I thought it was funny, so I implemented it :] |
---
### Installation
1. Firstly, ensure that Minecraft 1.12.2, Forge, and [Lambda](https://github.com/lambda-client/lambda) are installed.
2. [Download the plugin](https://github.com/0xTas/oasis/releases/tag/v1.1.2) or [build it from source](https://github.com/0xTas/oasis#contributing).
3. Place the JAR file in your `.minecraft/lambda/plugins` folder.
---
### Contributing
1. Clone the repository: `git clone https://github.com/0xTas/oasis`.
2. Follow the [IDE Setup Instructions](https://github.com/lambda-client/ExamplePlugin#setup-ide) from the Lambda Example-Plugin repo.
3. Ensure that `Oasis > Tasks > build > build` runs successfully.
4. You're all set!
---
### What's Next?

I plan to support this plugin for the foreseeable future, as I get regular use out of it myself.<br>
New modules may be added from time-to-time as I think of them and get them to a shareable state.<br>
If you encounter any bugs or have any suggestions, feel free to [open an issue](https://github.com/0xTas/oasis/issues/new), and I'll address it when I can.
<br><br>
> ### Disclaimer
> This plugin modifies your in-game experience in a way that may be unfair in a multiplayer environment.<br>
> Do not use this plugin on servers without permission from the admins.<br>
> By using this plugin, you agree to take responsibility for any bans or disciplinary actions taken against you in response.
<br><br>
### P.S.
If you find my code unpalatable, consider going easy on me.<br>
This project represents 90% of my entire experience with Java, Kotlin, and modding Minecraft.<br>
I'm always looking to learn and improve :3