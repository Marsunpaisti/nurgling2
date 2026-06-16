# KFC Duck Support Design

## Scope

KFC manages chickens and ducks in existing `chicken` and `incubator` areas using existing `gfx/terobjs/chickencoop` containers. Chicken behavior remains supported.

## Design

- Classify coop items by exact inventory resource path, not broad display aliases, for live birds, babies, eggs, dead birds, plucked birds, and cleaned carcasses.
- Track breeder coop quality per species, so chicken male/female comparisons never compare against duck male/female quality.
- Promote adult incubator birds into worse breeder coops only within same species and sex.
- Move chicks and ducklings from breeder coops to incubator coops, counting both against existing 24 baby limit. Excess babies use existing `Wring neck` to `A Bloody Mess` disposal flow.
- Dispose low-quality eggs by species threshold: chicken eggs against best chicken female threshold, duck eggs against best duck female threshold.
- Butcher using species-specific resource waits for dead, plucked, and cleaned states. `skipPluckingCocksInKFC` applies only to chicken cocks; duck drakes are plucked.
- Add duck eggs, carcasses, poultry meat, and feathers to generic VSpec categories. Duck feather is not a Fine Feather.

## Verification

- Compile KFC duck classification test.
- Run `ant jar` after implementation.
