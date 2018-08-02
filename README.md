# Notification broker - Portfolio project

## Description

This project describes a solution to the problem of delivering notifications, that are generated in the database and then delivered to the end user, using scalable backend.

### Technological stack

MS SQL Server, C#, Java/Spring, WebSocket, Node.js, Socket.io, Angular 6

## Assumptions

*   Database is MS SQL Server, Backend is written in Node.js
*   The backend is scalable and one user can be logged into two backend units.
*   Communication will take place using WebSockets.
*   The delivery time (latency) of notifications is important.
*   It is permissible that in exceptional cases the notification will not be delivered.
*   The system should not generate a high consumption of resources, and especially shouldn't overload the database.


## Solution

The application that connects to the database is a program written in Java Spring Boot. This program has the code name NTB. This program communicates with the database in a synchronous and asynchronous way. The database communicates asynchronously with NTB by sending UDP packets to the localhost where NTB is running. The database is additionally queried every 10 seconds for data, in order to prevent hypothetical loss of data, due to the fact that the UDP protocols do not guarantee the delivery of data. The risk of failure to deliver data to NTB is replaced by delivering them to the maximum 10 seconds later by polling.

Backends in Node.js are connected using WebSocket with NTB and the data is pushed to the backend. The message sent is in the JSON format:
```json
{
    "id":"notification id",
    "uid":"user id",
    "action":"action",
    "details":"detail1;detail2;..."
}
```
If the user is logged in to a specific backend, the message is forwarded to the user and at the same time a notification message of the JSON format is sent to NTB:
```json
{
    "commit":"notification id"
}
```
If NTB receives a notification delivery message, it sends a request to the database to remove the notification from the notifications table.

### Other possible solutions to the problem

*   Using the built-in MSSQL Notification Services and writing NTB in MS C#.
*   Using the RabbitMQ framework


# Technical details

## A. Diagram of the system

![diagram][logo]

[logo]: diagram.png "Diagram od the system"

## B. How to send UDP package by MSSQL
*   Create CLR function in C# MSSQL Database project and compile it to DLL file.

```csharp
using System.Data.SqlTypes;
using System.Net.Sockets;
using System.Text;

public partial class UserDefinedFunctions
{
    static void SendUdp(int srcPort, string dstIp, int dstPort, byte[] data)
    {
        using (UdpClient c = new UdpClient(srcPort))
            c.Send(data, data.Length, dstIp, dstPort);
    }

    [Microsoft.SqlServer.Server.SqlFunction]
    public static SqlString udp(SqlString data)
    {
        SendUdp(11001, "127.0.0.1", 11000, Encoding.ASCII.GetBytes(data.ToString()));
        return "ok";
    }

}
```
*    Add DLL file as MSSQL assembly.
*   Create function base on function from assembly.

```sql
CREATE FUNCTION [dbo].udp(@data nvarchar(max)) RETURNS char(2)
EXTERNAL NAME  UdpProj.UserDefinedFunctions.udp 
```
*   Use function in database trigger on notifications table

```sql
-- =============================================
-- Author:	Radoslaw Jodlowski
-- Description:	This trigger send data from inserted row by UDP protocol to localhost
-- =============================================
ALTER TRIGGER [dbo].[TRG_UDP_send_after_insert]
   ON  [dbo].[notifications]
   AFTER INSERT
AS 
BEGIN
	Declare @id int;
	Declare @uid int;
	Declare @action varchar(50);
	Declare @details varchar(max);
	Declare @data varchar(max);
	
	Select @id=id, @uid=uid,@action=action,@details=details from inserted;

	Set @data = convert(varchar(max),@id) + '@' + convert(varchar(max),@uid) + '@' + @action + '@' + @details;

	exec dbo.udp @data;
END
```

## C. How to forward the message on backend (node.js)

```javascript
const WebSocket = require('ws');
const NtbUrl = 'ws://localhost:9010/notificationBroker';
var io = require('socket.io')(confFile.socket_port) //4000
{
  console.log('socket io on port ' + confFile.socket_port.magenta);
};

// common session with other connections in the express framework
io.use(function(socket, next) {
    sessionMiddleware(socket.request, socket.request.res, next);
});
app.use(sessionMiddleware);

io.on('connection', function(socket){
  console.log('a user connected' + socket.request.session.user_id);
  socket.on('disconnect', function () {
      console.log('a user ' + socket.request.session.user_id + 'disconnect');
  });
});

function socketOnOpenMessage(event)
{
  console.log('NTB socked connected'.bgGreen + ' to: ' + NtbUrl);
}

function socketErrorHandler(ex)
{
  ntbError = true;
  console.log("handled error".bgYellow.red);
  console.log(ex);
}

var reconnect = function()
{
    var ntbError = false;
    var ws = new WebSocket(NtbUrl);
    ws.on('error', socketErrorHandler);
    ws.onmessage = parseNotification;
    ws.onopen = socketOnOpenMessage;
}
reconnect();

function checkSocketConnection()
{
  
  if (ntbError == true || ws.readyState !== 1)
  {
    console.log('NTB trying reconnect...'.bgYellow);
    ws.removeAllListeners();
    ws.close();
    reconnect();
  }
  else
    io.emit('message','ping');
}
setInterval(checkSocketConnection,10000);

function parseNotification(event)
{
  var obj = JSON.parse(event.data);
  // Yes, this code is to be replaced by Hash based structure
  for (var i in io.sockets.connected)
  {
    var s = io.sockets.connected[i];
    if (s.request.session.user_id == obj.uid)
    {
      var nn = {action:obj.action, details:obj.details};
      s.emit('notification',JSON.stringify(nn));
      ws.send(JSON.stringify({commit:obj.id}));
    }
  }
}

```

## D. How to use notification message on frotnend

This is a part of real application (with some modifications). MUX2 is my other data service for user private data.
ToastrService is from `ngx-toastr` npm library.

```typescript
import { Injectable } from '@angular/core';
import * as socketIo from 'socket.io-client';
import { Router, RoutesRecognized} from '../../node_modules/@angular/router';
import { MUX2Service } from './mux2.service';
import { ToastrService } from 'ngx-toastr';

const SERVER_URL = 'http://localhost:4000';

@Injectable({
  providedIn: 'root'
})

export class NotificationService {

  private socket;
  private url:string='';
  constructor(private router:Router, private mux2:MUX2Service, private toastr: ToastrService) 
  { 
    this.socket = socketIo(SERVER_URL);
    
    this.socket.on('notification', data => this.processNotification(data));

    // this is quick walkaround of use ActivatedRoute in data service
    router.events.subscribe((data) => {
      if (data instanceof RoutesRecognized) {
        this.url = data.url;
      }
    });
  }

  processNotification(data:string)
  {
    let z:any = JSON.parse(data);
    let action:string = z.action;
    let details:string = z.details;
    let t:string[] = details.split(';');
    if (action=='fillAddress')
    {
      this.toastr.info('for ' + t[0] + ' address','New incoming address has been generated');
      if (this.url == '/wallet') this.mux2.APIrequestWalletData();
    }
    if (action=='hotInInsert')
    {
      if (this.url == '/wallet') this.mux2.APIrequestPendingTransactionsData();
      this.toastr.info(t[0] + ' ' + t[1],'New incoming withdraw')
    }
    if (action=='hotInUpdate' || action =='hotOutUpdate')
    {
      if (this.url == '/wallet') this.mux2.APIrequestPendingTransactionsData();
    }
    if (action=='exchange')
    {
      if (this.url == '/wallet') this.mux2.APIrequestWalletData();
      if (this.url == '/history') this.mux2.APIrequestHistoryData(0);
      this.toastr.info('exchanged ' + t[2] + ' ' + t[1] + ' for ' + t[5] + ' ' + t[4],'Exchange has been completed');
    }
    if (action=='walletRefresh')
    {
      if (this.url == '/wallet') this.mux2.APIrequestWalletData();
    }
    if (action=='userRefresh')
    {
      this.mux2.APIrequestUserData();
    }
  }
}

```