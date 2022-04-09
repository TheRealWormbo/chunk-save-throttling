# chunk-save-throttling
A Minecraft mod for the Forge mod loader, aiming to specifically fix a performance issue in version 1.18.1 caused by saving chunks way too often.

This mod essentially applies the same fix for the issue as 1.18.2 does: Without affecting full world saving, individual chunks cannot be saved again for 10 seconds after the previous time they were written to disk. This should cut down severely on the amount of disk write activity of the single player game or server.