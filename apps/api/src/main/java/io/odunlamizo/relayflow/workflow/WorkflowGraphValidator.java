package io.odunlamizo.relayflow.workflow;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Runs when a workflow is enabled so incomplete drafts can still be saved. */
@Component
public class WorkflowGraphValidator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowGraphValidator.class);

    private static final Set<String> KNOWN_TRIGGER_EVENTS = Set.of("conversation_opened", "manual");

    public void validate(Map<String, Object> graph) {
        if (graph == null || graph.isEmpty()) {
            throw new WorkflowValidationException("Workflow has no graph configured");
        }

        List<Map<String, Object>> nodes = parseNodes(graph);
        List<Map<String, Object>> edges = parseEdges(graph);

        log.debug(
                "Validating workflow graph {} {}",
                kv("nodeCount", nodes.size()),
                kv("edgeCount", edges.size()));

        List<Map<String, Object>> triggerNodes =
                nodes.stream()
                        .filter(node -> NodeType.TRIGGER.getValue().equals(node.get("type")))
                        .toList();

        if (triggerNodes.isEmpty()) {
            throw new WorkflowValidationException("Workflow must have a trigger node");
        }

        if (triggerNodes.size() > 1) {
            throw new WorkflowValidationException("Workflow must have exactly one trigger node");
        }

        Map<String, Object> triggerNode = triggerNodes.getFirst();

        String event = (String) data(triggerNode).get("event");

        if (event == null || event.isBlank()) {
            throw new WorkflowValidationException("Trigger node has no event configured");
        }

        if (!KNOWN_TRIGGER_EVENTS.contains(event)) {
            throw new WorkflowValidationException(
                    "Unknown trigger event '" + event + "'. Known events: " + KNOWN_TRIGGER_EVENTS);
        }

        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");

            if (NodeType.fromValue(type).isEmpty()) {
                throw new WorkflowValidationException("Unknown node type '" + type + "'");
            }
        }

        String triggerId = (String) triggerNode.get("id");
        Set<String> reachable = bfsReachable(triggerId, nodes, edges);

        for (Map<String, Object> node : nodes) {
            String nodeId = (String) node.get("id");

            if (!triggerId.equals(nodeId) && !reachable.contains(nodeId)) {
                String label = labelOf(node);
                throw new WorkflowValidationException(
                        "Node "
                                + label
                                + " is not connected to the workflow — remove it or link it");
            }
        }

        for (Map<String, Object> node : nodes) {
            if (NodeType.CONDITION.getValue().equals(node.get("type"))) {
                validateConditionBranches(node, edges);
            }
        }

        for (Map<String, Object> node : nodes) {
            String type = (String) node.get("type");

            if (NodeType.SEND_MESSAGE.getValue().equals(type)) {
                validateRequiredField(
                        node, "message", "Send Message node has no message configured");
            }

            if (NodeType.WAIT_FOR_REPLY.getValue().equals(type)) {
                validateRequiredField(
                        node, "question", "Ask Question node has no question configured");
                validateWaitForReplyOptions(node, edges);
            }

            if (NodeType.JUMP_TO.getValue().equals(type)) {
                validateRequiredField(
                        node, "targetNodeId", "Jump To node has no target configured");
                validateJumpToTarget(node, nodes);
            }

            if (NodeType.HTTP_REQUEST.getValue().equals(type)) {
                validateRequiredField(node, "url", "HTTP Request node has no URL configured");
            }

            if (NodeType.SET_VARIABLE.getValue().equals(type)) {
                validateRequiredField(
                        node, "variableName", "Set Variable node has no variable name configured");
            }

            if (NodeType.SET_CONTACT_FIELD.getValue().equals(type)) {
                validateRequiredField(
                        node, "fieldKey", "Set Contact Field node has no field selected");
            }

            if (NodeType.SET_CONTACT_TAG.getValue().equals(type)) {
                validateRequiredField(node, "tagKey", "Set Contact Tag node has no tag selected");
            }

            if (NodeType.CONDITION.getValue().equals(type)) {
                validateConditionRows(node);
            }
        }

        log.debug("Workflow graph validation passed {}", kv("nodeCount", nodes.size()));
    }

    private void validateConditionBranches(
            Map<String, Object> node, List<Map<String, Object>> edges) {
        String nodeId = (String) node.get("id");
        List<Map<String, Object>> branches = branches(node);

        if (branches.isEmpty()) {
            throw new WorkflowValidationException(
                    "Condition node " + labelOf(node) + " has no branches configured");
        }

        Set<String> connectedHandles =
                edges.stream()
                        .filter(edge -> nodeId.equals(edge.get("source")))
                        .map(edge -> (String) edge.get("sourceHandle"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (Map<String, Object> branch : branches) {
            String branchId = (String) branch.get("id");
            String branchLabel =
                    branch.get("label") != null ? (String) branch.get("label") : branchId;

            if (!connectedHandles.contains(branchId)) {
                throw new WorkflowValidationException(
                        "Condition branch '"
                                + branchLabel
                                + "' on node "
                                + labelOf(node)
                                + " has no connected edge");
            }
        }
    }

    private void validateRequiredField(
            Map<String, Object> node, String field, String errorMessage) {
        Object value = data(node).get(field);

        if (value == null || value.toString().isBlank()) {
            throw new WorkflowValidationException(errorMessage + " (node " + labelOf(node) + ")");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateWaitForReplyOptions(
            Map<String, Object> node, List<Map<String, Object>> edges) {
        String responseType = (String) data(node).getOrDefault("responseType", "generic");

        if (!"defined".equals(responseType)) {
            return;
        }

        String nodeId = (String) node.get("id");
        List<Map<String, Object>> options =
                (List<Map<String, Object>>) data(node).getOrDefault("options", List.of());

        Set<String> connectedHandles =
                edges.stream()
                        .filter(edge -> nodeId.equals(edge.get("source")))
                        .map(edge -> (String) edge.get("sourceHandle"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (Map<String, Object> option : options) {
            String optionId = (String) option.get("id");
            String optionText = option.get("text") != null ? (String) option.get("text") : optionId;

            if (!connectedHandles.contains(optionId)) {
                throw new WorkflowValidationException(
                        "Ask Question option '"
                                + optionText
                                + "' on node "
                                + labelOf(node)
                                + " has no connected edge");
            }
        }

        if (!connectedHandles.contains("default")) {
            throw new WorkflowValidationException(
                    "Ask Question node " + labelOf(node) + " has no 'Other' edge connected");
        }
    }

    private void validateJumpToTarget(Map<String, Object> node, List<Map<String, Object>> nodes) {
        String targetId = (String) data(node).get("targetNodeId");
        String nodeId = (String) node.get("id");

        if (targetId.equals(nodeId)) {
            throw new WorkflowValidationException(
                    "Jump To node " + labelOf(node) + " cannot jump to itself");
        }

        boolean targetExists =
                nodes.stream().anyMatch(candidate -> targetId.equals(candidate.get("id")));

        if (!targetExists) {
            throw new WorkflowValidationException(
                    "Jump To node " + labelOf(node) + " references a node that no longer exists");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateConditionRows(Map<String, Object> node) {
        List<Map<String, Object>> branches = branches(node);

        for (int index = 0; index < branches.size() - 1; index++) {
            Map<String, Object> branch = branches.get(index);
            List<Map<String, Object>> conditions =
                    (List<Map<String, Object>>) branch.getOrDefault("conditions", List.of());
            String branchLabel =
                    branch.get("label") != null
                            ? (String) branch.get("label")
                            : "branch " + (index + 1);

            if (conditions.isEmpty()) {
                throw new WorkflowValidationException(
                        "Condition branch '"
                                + branchLabel
                                + "' on node "
                                + labelOf(node)
                                + " has no conditions configured");
            }

            for (Map<String, Object> condition : conditions) {
                String variable = (String) condition.get("variable");

                if (variable == null || variable.isBlank()) {
                    throw new WorkflowValidationException(
                            "A condition in branch '"
                                    + branchLabel
                                    + "' on node "
                                    + labelOf(node)
                                    + " has no variable selected");
                }
            }
        }
    }

    private Set<String> bfsReachable(
            String startId, List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, List<String>> adjacency = new HashMap<>();

        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get("source");
            String target = (String) edge.get("target");

            if (source != null && target != null) {
                adjacency.computeIfAbsent(source, key -> new ArrayList<>()).add(target);
            }
        }

        // jumpTo nodes route through their data, so their targets count as directed links.
        for (Map<String, Object> node : nodes) {
            if (NodeType.JUMP_TO.getValue().equals(node.get("type"))) {
                String nodeId = (String) node.get("id");
                String targetId = (String) data(node).get("targetNodeId");

                if (targetId != null && !targetId.isBlank()) {
                    adjacency.computeIfAbsent(nodeId, key -> new ArrayList<>()).add(targetId);
                }
            }
        }

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (visited.add(current)) {
                queue.addAll(adjacency.getOrDefault(current, List.of()));
            }
        }

        return visited;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseNodes(Map<String, Object> graph) {
        return (List<Map<String, Object>>) graph.getOrDefault("nodes", List.of());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseEdges(Map<String, Object> graph) {
        return (List<Map<String, Object>>) graph.getOrDefault("edges", List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> node) {
        return (Map<String, Object>) node.getOrDefault("data", Map.of());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> branches(Map<String, Object> node) {
        return (List<Map<String, Object>>) data(node).getOrDefault("branches", List.of());
    }

    private String labelOf(Map<String, Object> node) {
        Object label = data(node).get("label");

        return label != null ? "'" + label + "'" : "(id: " + node.get("id") + ")";
    }
}
