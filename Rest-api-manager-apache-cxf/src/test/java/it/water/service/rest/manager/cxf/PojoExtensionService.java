package it.water.service.rest.manager.cxf;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.service.EntityExtensionService;
import it.water.core.interceptors.annotations.FrameworkComponent;

@FrameworkComponent(properties = EntityExtensionService.RELATED_ENTITY_PROPERTY + "=it.water.service.rest.manager.cxf.TestPojo")
public class PojoExtensionService implements EntityExtensionService {

    @Override
    public Class<? extends BaseEntity> relatedType() {
        return TestPojo.class;
    }

    @Override
    public Class<? extends BaseEntity> type() {
        return TestPojoExtension.class;
    }
}
