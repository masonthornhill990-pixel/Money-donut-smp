# Donut Payment FX (Fabric, Minecraft 1.21.11)

## Get the .jar (no coding needed)
1. Make a free GitHub account, create a new repo, upload ALL files from this folder (include the hidden .github folder).
2. Repo -> Actions tab -> "build" -> Run workflow.
3. When it finishes, download the `donutfx-jar` artifact, unzip it -> donutfx-1.0.0.jar.

## Install
Modrinth App -> your Fabric 1.21.11 profile -> Add content -> From file. Also needs Fabric API.

## Tiers (same animation for everything inside a range; under 10M = nothing)
1: 10M-99M (just the top-right banner with name + amount, nothing else) | 2: 100M-249M | 3: 250M-499M | 4: 500M-999M | 5: 1B-4B | 6: 5B-9B | 7: 10B-99B | 8: 100B+

## Test it in-game
/donutfx test 10m   100m   250m   500m   1b   5b   10b   100b

## Make it match DonutSMP
Edit config/donutfx.json: "regex" must have named groups (?<name>...), (?<amount>...), optional (?<suffix>...).
Then run /donutfx reload. Tier minimums are the "tiers" list in the same file.
