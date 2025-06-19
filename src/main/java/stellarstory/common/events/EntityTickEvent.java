package stellarstory.common.events;


import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityEvent;

public class EntityTickEvent extends EntityEvent {

    public static class Pre extends EntityTickEvent {
        public Pre(Entity entity) {
            super(entity);
        }
    }

    public static class Post extends EntityTickEvent {
        public Post(Entity entity) {
            super(entity);
        }
    }

    public EntityTickEvent(Entity entity) {
        super(entity);
    }
}
