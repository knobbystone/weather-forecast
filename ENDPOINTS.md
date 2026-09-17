# Testable Endpoints

## main
Hardcoded to MLB/33,70 (Cape Canaveral).

```bash
curl -s "http://localhost:8080/api/forecast/today" | jq .
```

---

## feature/parameterized-gridpoint
Takes office and grid coords as query params.

```bash
# Cape Canaveral
curl -s "http://localhost:8080/api/forecast/today?office=MLB&gridX=33&gridY=70" | jq .

# Seattle
curl -s "http://localhost:8080/api/forecast/today?office=SEW&gridX=125&gridY=68" | jq .

# Chicago
curl -s "http://localhost:8080/api/forecast/today?office=LOT&gridX=76&gridY=73" | jq .
```

---

## feature/flux-examples
Three endpoints — same hardcoded MLB/33,70 gridpoint.

```bash
# Today only (Mono)
curl -s "http://localhost:8080/api/forecast/today" | jq .

# Full week, daytime periods (Flux)
curl -s "http://localhost:8080/api/forecast/week" | jq .

# Batch — multiple gridpoints, results interleaved (Flux fan-out)
curl -s "http://localhost:8080/api/forecast/today/batch?gridpoint=MLB/33,70&gridpoint=SEW/125,68&gridpoint=LOT/76,73" | jq .
```
