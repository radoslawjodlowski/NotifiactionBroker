package pl.raaadziu.nb1;

import org.json.JSONObject;
import java.util.Map;

public class Notification
{
    public Integer id;
    public Integer uid;
    public String action;
    public String details;

    public Integer getId() {
        return id;
    }

    public Integer getUid() {
        return uid;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public Notification(Map<String, Object> map)
    {
        id = (Integer) map.get("id");
        uid = (Integer) map.get("uid");
        action = (String) map.get("action");
        details = (String) map.get("details");
    }

    public Notification(String buffer)
    {
        String[] t = buffer.split("@");
        id = Integer.parseInt(t[0]);
        uid = Integer.parseInt(t[1]);
        action = t[2];
        details = t[3];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Notification) {
            Notification otherNotification = (Notification) obj;
            return (id == otherNotification.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public String toJSONString()
    {
        return new JSONObject(this).toString();
    }

}
