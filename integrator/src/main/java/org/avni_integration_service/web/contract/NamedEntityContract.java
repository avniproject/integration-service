package org.avni_integration_service.web.contract;

import org.avni_integration_service.integration_data.domain.framework.BaseEnum;
import org.avni_integration_service.integration_data.domain.framework.NamedEntity;

public class NamedEntityContract extends BaseEntityContract {
    private String name;

    public NamedEntityContract(BaseEnum baseEnum) {
        super(baseEnum.getValue());
        this.name = baseEnum.name();
    }

    public NamedEntityContract(int id, String name) {
        super(id);
        this.name = name;
    }

    public NamedEntityContract(int id, String uuid, String name) {
        super(id, uuid);
        this.name = name;
    }

    public NamedEntityContract(NamedEntity namedEntity) {
        super(namedEntity.getId(), namedEntity.getUuid());
        this.name = namedEntity.getName();
    }

    public NamedEntityContract() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static NamedEntityContract fromNamedEntity(NamedEntity namedEntity) {
        NamedEntityContract namedEntityContract = new NamedEntityContract();
        namedEntityContract.setUuid(namedEntity.getUuid());
        namedEntityContract.setName(namedEntity.getName());
        return namedEntityContract;
    }
}
