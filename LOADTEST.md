# Нагрузочное тестирование и профилирование до модификации
Тестирование проводилось с помощью утилты wrk на Windows 10.

![system](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/system.JPG)

Профилирование проводилось с помощью JProfiler ``11.0(2019-03-19)``

## Метод PUT
### Утилита wrk
```
./wrk --latency -c4 -t4 -d1m -s ../scripts/put.lua http://localhost:8060
Running 1m test @ http://localhost:8060
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    13.46ms    7.94ms  85.20ms   83.40%
    Req/Sec    77.12     31.87   171.00     68.95%
  Latency Distribution
     50%   11.63ms
     75%   16.55ms
     90%   23.46ms
     99%   42.18ms
  18470 requests in 1.00m, 1.18MB read
Requests/sec:    307.46
Transfer/sec:     20.12KB
```

Как видим, скорость работы сервера очень мала(20к запросов в секунду). Для того, чтобы выяснить проблему проведём профилирование и выясним, что затрудняет работу.

![PUT](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/put_before)

#Модификация
Проведя профилирование, было выявлено, что наибольшее время тратится на запись данных в файл. Это происходит из-за использования FileOutputStream, что не очень выгодно по эффективности для небольших файлов.

Было решено перейти на базу данных(LevelDb от Google).

Сделаем новые замеры. 

#После модификации
## Метод PUT
### Утилита wrk
```
./wrk --latency -c4 -t4 -d1m -s ../scripts/put.lua http://localhost:8060
Running 1m test @ http://localhost:8060
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   524.00us    2.57ms  84.72ms   99.36%
    Req/Sec     2.60k   329.22     3.66k    87.52%
  Latency Distribution
     50%  342.00us
     75%  375.00us
     90%  426.00us
     99%    2.14ms
  465883 requests in 1.00m, 29.77MB read
Requests/sec:   7759.99
Transfer/sec:    507.73KB
```
По результатам видно, что количество запросов в секунду увеличилось, что подтверждает гипотезу выше.

![PUT20](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/put_after.JPG)

![PUT21](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/put_after_classes.JPG)


## Метод GET
### Утилита wrk
```
./wrk --latency -c4 -t4 -d1m -s ../scripts/get.lua http://localhost:8060
Running 1m test @ http://localhost:8060
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.79ms    1.74ms   8.02ms   85.25%
    Req/Sec   152.50    160.49   390.00     75.00%
  Latency Distribution
     50%    1.21ms
     75%    2.13ms
     90%    3.89ms
     99%    8.02ms
  61 requests in 1.00m, 3.81KB read
  Non-2xx or 3xx responses: 61
Requests/sec:      1.02
Transfer/sec:      64.97B
```

![GET21](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/get_after_classes.JPG)

## Метод DELETE
### Утилита wrk
```
./wrk --latency -c4 -t4 -d1m -s ../scripts/delete.lua http://localhost:8060
Running 1m test @ http://localhost:8060
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     0.96ms   11.69ms 401.41ms   99.36%
    Req/Sec     3.73k   558.06     5.05k    87.73%
  Latency Distribution
     50%  232.00us
     75%  258.00us
     90%  298.00us
     99%  632.00us
  221539 requests in 1.00m, 14.37MB read
Requests/sec:   3688.87
Transfer/sec:    244.96KB
```

![DEL20](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/delete_after.JPG)

![DEL21](https://github.com/Xerocry/2018-highload-kv/blob/master/pics/delete_after_classes.JPG)


