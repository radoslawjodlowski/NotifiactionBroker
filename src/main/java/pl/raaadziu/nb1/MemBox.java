package pl.raaadziu.nb1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MemBox {

    private SqlApi sqlApi;
    private SessionsContainer sessionsContainer;
    private Integer lastId = 0;
    private ArrayList<Notification> backList = new ArrayList<>();
    private Object mutex = new Object();
    private static Logger log = LoggerFactory.getLogger("MemBox");
    @Autowired
    public MemBox(SqlApi sqlApi, SessionsContainer sessionsContainer)
    {
        this.sqlApi = sqlApi;
        this.sessionsContainer = sessionsContainer;
    }

    public void broadcastFromUtp(Notification notification)
    {
        synchronized (mutex)
        {
            String text = notification.toJSONString();
            sessionsContainer.broadCastToAll(text);
            log.debug("U=>" + text);
            backList.add(notification);
        }
    }
    synchronized void Refresh() throws SqlApiException
    {
        //get
        List<Map<String, Object>> mapList = sqlApi.query("exec web.getNotificationsV2 @fromId=" + lastId);
        //broadcast
        synchronized (mutex)
        {
            for (Map<String, Object> aMapList : mapList)
            {
                Notification a = new Notification((aMapList));
                if (a.id > lastId) lastId = a.id;
                if (!backList.contains(a))
                {
                    backList.add(a);
                    String text = a.toJSONString();
                    log.debug("R=>" + text);
                    sessionsContainer.broadCastToAll(text);
                }
            }
            backList.clear();
        }

    }

    void commitById(Integer id) throws SqlApiException
    {
        sqlApi.query("exec web.commitNotification @Id=" + id);
    }

}
