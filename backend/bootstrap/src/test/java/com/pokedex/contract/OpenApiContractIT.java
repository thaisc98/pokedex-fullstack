package com.pokedex.contract;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.pokedex.support.AbstractIntegrationIT;
import io.restassured.path.json.JsonPath;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Contract check: what the running app documents at {@code /v3/api-docs} (springdoc) must agree with the
 * hand-written contract {@code docs/api/openapi.yaml} (docs/agents/README.md §2).
 *
 * <p>Direction: <b>implemented ⊆ contract</b>. Every path + method the app serves must exist in the contract
 * with the same path/query parameter names, the same set of response status codes, and a success response whose
 * schema has the same property names (compared recursively through {@code $ref}, {@code allOf} and array
 * {@code items}). Endpoints in the contract that are not built yet are ignored. The comparison is semantic:
 * descriptions, examples, formats, schema names and scalar types are not compared.
 *
 * <p>springdoc is enabled only in the {@code demo} profile (security.md §3.2), so this class runs with
 * {@code @ActiveProfiles("demo")} in its own context. In that profile the docs and Swagger UI are reachable
 * anonymously.
 */
@ActiveProfiles("demo")
@DisplayName("Contract - /v3/api-docs matches docs/api/openapi.yaml")
class OpenApiContractIT extends AbstractIntegrationIT {

    private static final Path CONTRACT = Path.of(System.getProperty("basedir", System.getProperty("user.dir")))
            .resolve("../../docs/api/openapi.yaml")
            .normalize();

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

    private static final int MAX_SCHEMA_DEPTH = 8;

    private static Map<String, Object> contract;

    @BeforeAll
    static void loadContract() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONTRACT, StandardCharsets.UTF_8)) {
            contract = asMap(new Yaml(new SafeConstructor(new LoaderOptions())).load(reader));
        }
        assertThat(contract).as("contract %s", CONTRACT).containsKey("paths");
    }

    @Test
    void should_serve_api_docs_and_swagger_ui_anonymously_in_demo_profile() {
        given().when().get("/v3/api-docs").then().statusCode(200).contentType(containsString("json"));
        given().when().get("/swagger-ui/index.html").then().statusCode(200);
    }

    @Test
    void should_document_only_operations_that_match_the_contract() {
        Map<String, Object> implemented = fetchApiDocs();

        assertThat(operations(implemented, ""))
                .as("springdoc documents the implemented endpoints (non-vacuous check)")
                .containsKey("GET /api/v1/pokemon");
        assertThat(mismatches(implemented, "", contract))
                .as("implemented API vs docs/api/openapi.yaml")
                .isEmpty();
    }

    // ------------------------------------------------------------ comparator self-checks (no app needed)

    @Test
    void should_find_no_mismatch_when_the_contract_is_compared_with_itself() {
        String base = contractBasePath(contract);

        assertThat(operations(contract, base)).containsKey("GET /api/v1/pokemon");
        assertThat(operations(contract, base).get("GET /api/v1/pokemon").successShape())
                .as("allOf + $ref resolution reaches PokemonSummaryDto and Ability")
                .hasToString("{content=[array, {abilities=[array, {hidden=scalar, name=scalar}], category=scalar,"
                        + " id=scalar, mass=scalar, name=scalar, sprite=scalar}], page=scalar, size=scalar,"
                        + " totalElements=scalar, totalPages=scalar}");
        assertThat(operations(contract, base))
                .as("US02 detail is in the contract, so the check covers it as soon as the app serves it")
                .containsKey("GET /api/v1/pokemon/{}");
        assertThat(String.valueOf(
                        operations(contract, base).get("GET /api/v1/pokemon/{}").successShape()))
                .as("detail shape reaches the evolution tree and every trigger variant")
                .contains("artwork=scalar", "evolution={root={evolvesTo=[array,", "speciesId=scalar")
                .contains("{item=scalar, kind=scalar}", "{kind=scalar, minLevel=scalar}")
                .contains("{kind=scalar, name=scalar}", "{kind=scalar}");
        assertThat(mismatches(contract, base, contract)).isEmpty();
    }

    @Test
    void should_report_a_mismatch_when_an_implemented_operation_drifts_from_the_contract() {
        String json = """
                {"openapi":"3.1.0","paths":{"/api/v1/pokemon":{"get":{
                  "parameters":[{"name":"page","in":"query"},{"name":"pageSize","in":"query"}],
                  "responses":{"200":{"content":{"application/json":{"schema":{"type":"object","properties":{
                    "content":{"type":"array","items":{"type":"object","properties":{"id":{"type":"integer"}}}},
                    "page":{"type":"integer"}}}}}},"400":{},"500":{}}}},
                  "/api/v1/not-in-contract":{"get":{"responses":{"200":{}}}}}}
                """;
        Map<String, Object> drifted = asMap(new JsonPath(json).getMap("$"));

        assertThat(mismatches(drifted, "", contract))
                .anySatisfy(m -> assertThat(m).startsWith("GET /api/v1/not-in-contract: served by the app"))
                .anySatisfy(m -> assertThat(m).startsWith("GET /api/v1/pokemon: parameters"))
                .anySatisfy(m -> assertThat(m).startsWith("GET /api/v1/pokemon: status codes"))
                .anySatisfy(m -> assertThat(m).startsWith("GET /api/v1/pokemon: success schema properties"));
    }

    /** implemented ⊆ contract; one human-readable line per difference. */
    static List<String> mismatches(
            Map<String, Object> implemented, String implementedBasePath, Map<String, Object> spec) {
        Map<String, Operation> contractOps = operations(spec, contractBasePath(spec));
        List<String> mismatches = new ArrayList<>();
        operations(implemented, implementedBasePath).forEach((key, impl) -> {
            Operation expected = contractOps.get(key);
            if (expected == null) {
                mismatches.add(key + ": served by the app but missing from openapi.yaml");
                return;
            }
            if (!impl.parameters().equals(expected.parameters())) {
                mismatches.add(key + ": parameters " + impl.parameters() + " != contract " + expected.parameters());
            }
            if (!impl.statuses().equals(expected.statuses())) {
                mismatches.add(key + ": status codes " + impl.statuses() + " != contract " + expected.statuses());
            }
            if (!impl.successShape().equals(expected.successShape())) {
                mismatches.add(key + ": success schema properties " + impl.successShape() + " != contract "
                        + expected.successShape());
            }
        });
        return mismatches;
    }

    // ------------------------------------------------------------ extraction

    /** One operation reduced to what the contract check compares. */
    record Operation(Set<String> parameters, Set<String> statuses, Object successShape) {}

    private static Map<String, Object> fetchApiDocs() {
        String json = given().when()
                .get("/v3/api-docs")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        return asMap(new JsonPath(json).getMap("$"));
    }

    /** The contract's paths are relative to {@code servers[0].url} ({@code /api/v1}). */
    private static String contractBasePath(Map<String, Object> doc) {
        Object servers = doc.get("servers");
        if (servers instanceof List<?> list && !list.isEmpty()) {
            Object url = asMap(list.getFirst()).get("url");
            if (url instanceof String s && s.startsWith("/")) {
                return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
            }
        }
        return "";
    }

    /** {@code "GET /api/v1/pokemon/{}"} → operation; path-template variable names are normalised to {@code {}}. */
    private static Map<String, Operation> operations(Map<String, Object> doc, String basePath) {
        Map<String, Operation> result = new TreeMap<>();
        asMap(doc.get("paths")).forEach((path, item) -> {
            Map<String, Object> pathItem = asMap(item);
            List<Object> shared = asList(pathItem.get("parameters"));
            pathItem.forEach((method, op) -> {
                if (!HTTP_METHODS.contains(method)) {
                    return;
                }
                Map<String, Object> operation = asMap(op);
                List<Object> params = new ArrayList<>(shared);
                params.addAll(asList(operation.get("parameters")));
                String key = method.toUpperCase() + " " + (basePath + path).replaceAll("\\{[^}]+}", "{}");
                result.put(
                        key,
                        new Operation(
                                parameterNames(doc, params),
                                new TreeSet<>(asMap(operation.get("responses")).keySet()),
                                successShape(doc, asMap(operation.get("responses")))));
            });
        });
        return result;
    }

    private static Set<String> parameterNames(Map<String, Object> doc, List<Object> params) {
        Set<String> names = new TreeSet<>();
        for (Object p : params) {
            Map<String, Object> param = resolve(doc, asMap(p));
            Object in = param.get("in");
            if ("path".equals(in) || "query".equals(in)) {
                names.add(in + ":" + param.get("name"));
            }
        }
        return names;
    }

    private static Object successShape(Map<String, Object> doc, Map<String, Object> responses) {
        for (var entry : new TreeMap<>(responses).entrySet()) {
            if (!entry.getKey().startsWith("2")) {
                continue;
            }
            Map<String, Object> content =
                    asMap(resolve(doc, asMap(entry.getValue())).get("content"));
            if (content.isEmpty()) {
                return "no-content";
            }
            Object media = content.containsKey("application/json")
                    ? content.get("application/json")
                    : content.values().iterator().next();
            return shape(doc, asMap(asMap(media).get("schema")), 0);
        }
        return "no-success-response";
    }

    /**
     * Structural shape of a schema: an object becomes a sorted map of property name → shape, an array becomes
     * {@code ["array", itemShape]}, anything else is {@code "scalar"}. Follows {@code $ref}, merges {@code allOf}
     * (later members override earlier ones, as the contract's {@code PageResponse<T>} specialisation does), and
     * unwraps a {@code oneOf}/{@code anyOf} that only adds {@code null}.
     */
    private static Object shape(Map<String, Object> doc, Map<String, Object> rawSchema, int depth) {
        if (depth > MAX_SCHEMA_DEPTH) {
            return "recursive";
        }
        Map<String, Object> schema = merged(doc, rawSchema);
        // A real choice between several schemas (e.g. EvolutionTrigger's four `kind`s): compare every option.
        for (String alternative : List.of("oneOf", "anyOf")) {
            List<Object> options = nonNullOptions(doc, schema, alternative);
            if (options.size() > 1) {
                return Map.of(
                        alternative,
                        options.stream()
                                .map(o -> shape(doc, asMap(o), depth + 1))
                                .map(String::valueOf)
                                .sorted()
                                .toList());
            }
        }
        Object type = schema.get("type");
        boolean isArray = "array".equals(type) || (type instanceof Collection<?> c && c.contains("array"));
        if (isArray) {
            return List.of("array", shape(doc, asMap(schema.get("items")), depth + 1));
        }
        Map<String, Object> properties = asMap(schema.get("properties"));
        if (properties.isEmpty()) {
            return "scalar";
        }
        Map<String, Object> result = new TreeMap<>();
        properties.forEach((name, child) -> result.put(name, shape(doc, asMap(child), depth + 1)));
        return result;
    }

    private static Map<String, Object> merged(Map<String, Object> doc, Map<String, Object> rawSchema) {
        Map<String, Object> schema = resolve(doc, rawSchema);
        // Unwrap "X or null" (possibly nested, e.g. trigger: oneOf[EvolutionTrigger, null]) until stable.
        boolean unwrapped = true;
        for (int hops = 0; unwrapped && hops < 10; hops++) {
            unwrapped = false;
            for (String alternative : List.of("oneOf", "anyOf")) {
                List<Object> options = nonNullOptions(doc, schema, alternative);
                if (options.size() == 1) {
                    schema = asMap(options.getFirst());
                    unwrapped = true;
                }
            }
        }
        List<Object> allOf = asList(schema.get("allOf"));
        if (allOf.isEmpty()) {
            return schema;
        }
        Map<String, Object> result = new LinkedHashMap<>(schema);
        result.remove("allOf");
        Map<String, Object> properties = new LinkedHashMap<>(asMap(schema.get("properties")));
        for (Object member : allOf) {
            Map<String, Object> part = merged(doc, asMap(member));
            properties.putAll(asMap(part.get("properties")));
            part.forEach((k, v) -> {
                if (!k.equals("properties")) {
                    result.putIfAbsent(k, v);
                }
            });
        }
        result.put("properties", properties);
        return result;
    }

    private static List<Object> nonNullOptions(Map<String, Object> doc, Map<String, Object> schema, String key) {
        return asList(schema.get(key)).stream()
                .map(o -> (Object) resolve(doc, asMap(o)))
                .filter(o -> !"null".equals(asMap(o).get("type")))
                .toList();
    }

    /** Follows a local {@code $ref} such as {@code #/components/schemas/PageResponse}. */
    private static Map<String, Object> resolve(Map<String, Object> doc, Map<String, Object> node) {
        Map<String, Object> current = node;
        for (int hops = 0; hops < 20 && current.get("$ref") instanceof String ref; hops++) {
            assertThat(ref).as("only local $refs are supported").startsWith("#/");
            Map<String, Object> target = doc;
            for (String segment : ref.substring(2).split("/")) {
                target = asMap(target.get(segment.replace("~1", "/").replace("~0", "~")));
            }
            current = target;
        }
        return current;
    }

    // ------------------------------------------------------------ untyped YAML/JSON helpers

    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // YAML keys such as 200 may load as Integer; normalise every key to a String.
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    private static List<Object> asList(Object value) {
        return value instanceof List<?> list ? new ArrayList<>(list) : List.of();
    }
}
