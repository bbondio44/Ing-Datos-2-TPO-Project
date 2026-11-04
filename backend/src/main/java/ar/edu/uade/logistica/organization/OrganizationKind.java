package ar.edu.uade.logistica.organization;

enum OrganizationKind {
    SUPPLIER("proveedor", "id_proveedor", true),
    OPERATOR("operador_logistico", "id_operador", false);

    final String table;
    final String idColumn;
    final boolean serviceType;

    OrganizationKind(String table, String idColumn, boolean serviceType) {
        this.table = table;
        this.idColumn = idColumn;
        this.serviceType = serviceType;
    }
}
