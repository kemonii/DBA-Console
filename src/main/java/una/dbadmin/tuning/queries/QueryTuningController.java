package una.dbadmin.tuning.queries;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import una.dbadmin.common.OperationResult;

@Tag(name = "Query tuning")
@RestController
@RequestMapping("/api/tuning/queries")
public class QueryTuningController {
    private final ExecutionPlanService plans;
    private final StatisticsService statistics;
    private final IndexService indexes;
    public QueryTuningController(ExecutionPlanService plans, StatisticsService statistics, IndexService indexes) {
        this.plans = plans;
        this.statistics = statistics;
        this.indexes = indexes;
    }
    public record ExplainRequest(String statement) {}
    public record IndexRequest(String schema, String table, String name, List<String> columns, IndexService.IndexType type) {}
    @PostMapping("/plan")
    public OperationResult explain(@RequestBody ExplainRequest request) {
        return OperationResult.ok("Execution plan generated", plans.explain(request.statement()));
    }
    @GetMapping("/statistics/{schema}")
    public OperationResult statistics(@PathVariable String schema) {
        return OperationResult.ok("Statistics listed", statistics.list(schema));
    }
    @PostMapping("/statistics/{schema}/gather")
    public OperationResult gatherSchema(@PathVariable String schema) {
        statistics.gatherSchema(schema);
        return OperationResult.ok("Schema statistics gathered", null);
    }
    @PostMapping("/statistics/{schema}/{table}/gather")
    public OperationResult gatherTable(@PathVariable String schema, @PathVariable String table) {
        statistics.gatherTable(schema, table);
        return OperationResult.ok("Table statistics gathered", null);
    }
    @GetMapping("/indexes/{schema}")
    public OperationResult indexes(@PathVariable String schema) {
        return OperationResult.ok("Indexes listed", indexes.list(schema));
    }
    @PostMapping("/indexes")
    public OperationResult create(@RequestBody IndexRequest request) {
        indexes.create(request.schema(), request.table(), request.name(), request.columns(), request.type());
        return OperationResult.ok("Index created", null);
    }
    @PostMapping("/indexes/{schema}/{index}/rebuild")
    public OperationResult rebuild(@PathVariable String schema, @PathVariable String index) {
        indexes.rebuild(schema, index);
        return OperationResult.ok("Index rebuilt", null);
    }
    @PostMapping("/indexes/{schema}/{index}/validate")
    public OperationResult validate(@PathVariable String schema, @PathVariable String index) {
        return OperationResult.ok("Index structure validated", indexes.validate(schema, index));
    }
    @DeleteMapping("/indexes/{schema}/{index}")
    public OperationResult drop(@PathVariable String schema, @PathVariable String index, @RequestParam(defaultValue = "false") boolean confirm) {
        indexes.drop(schema, index, confirm);
        return OperationResult.ok("Index dropped", null);
    }
}
