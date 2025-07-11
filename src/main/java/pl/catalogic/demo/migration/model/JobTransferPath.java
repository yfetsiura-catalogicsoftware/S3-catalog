package pl.catalogic.demo.migration.model;

import java.util.List;

public record JobTransferPath(Node sourceNode, Node destinationNode) {

  public record Node(String id, String name, String type, List<Node> children) {}
}
