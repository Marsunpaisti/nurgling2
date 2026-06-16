# KFC Duck Support Plan

## Steps

1. Add KFC species/type metadata with exact resource matching for chicken and duck variants.
2. Refactor coop and incubator scans to build per-species quality lists from all `Chicken Coop` inventories.
3. Replace adult birds only within same species and sex; use held-item `dropOn` to avoid species display aliases.
4. Transfer chicks and ducklings together to incubators; cull overflow with existing bloody-mess flow.
5. Cull eggs using per-species best female threshold.
6. Butcher birds with species-specific resource wait conditions; keep chicken-cock pluck skip chicken-only.
7. Add duck products to VSpec categories and exclude duck feather from Fine Feather.
8. Verify with classification test compile and `ant jar`.

## Verification Result

- `javac ... KFCDuckSupportTest.java && java ... KFCDuckSupportTest`: passed with no output.
- `ant jar`: BUILD SUCCESSFUL. Resource packaging printed existing `Invalid number of decoded files for image` messages for HUD button `.res` files, but Ant completed and updated `build/hafen.jar`.
