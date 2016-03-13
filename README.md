# Реализация метода детектирования аномального поведения пользователей информационной системы на основе оценки результата выполнения SQL-запроса

[![](https://img.shields.io/badge/License-Apache%202-blue.svg)](LICENSE)

Библиотека, исходный код которой хранится в данном репозитории, представляет собой средство проактивной защиты информации и предназначена для обнаружения аномалий в поведении пользователей информационных систем (ИС), работающих на базе платформы Java SE, путём оценки результатов выполнения SQL-запросов к базам данных (БД).

Библиотека позволяет:
  * формировать профили нормального поведения пользователей ИС;
  * выполнять обнаружение аномальных SQL-запросов;
  * блокировать пользователям ИС доступ к данным в случае обнаружения аномального поведения;
  * оповещать администраторов ИС об обнаруженных аномалиях.

Использующийся метод обнаружения аномалий основан на оценке результата выполнения запроса путём вычисления плотности графа, отражающего взаимосвязи между данными, которые выбираются запросом.
Интеграция программы со сторонними информационными системами осуществляется на основе механизма экранирования JDBC-драйверов.
Возможна интеграция с системами обнаружения вторжений, поддерживающими информационный обмен в формате IDMEF.

В данный момент в репозитории хранится лишь базовое ядро для постороения детекторов систем обнаружения вторжений.
В будущем планируется опублировать примеры БД, на которых проводились эксперименты, а также примеры использования библиотеки в месте с реальными Java EE приложениями.

Данная библиотека разработана на основе методов и подходов, описанных в статьях:
  * Григоров А.С. Обнаружение аномалий в SQL-запросах к базам данных на основе оценки внутренней структуры результатов выполнения запросов. // Научно-технический вестник Поволжья. №6 2011 г. - Казань: Научно-технический вестник Поволжья, 2011. - С. 146-151.
  * Беляев А.В., Григоров А.С. Обнаружение атак на базы данных на основе оценки внутренней структуры результатов выполнения SQL-запросов. // Научно-технический вестник Поволжья. №2 2012 г. - Казань: Научно-технический вестник Поволжья, 2012. - С. 99-104.
  * Григоров А.С., Плашенков В.В. Метод обнаружения аномалий в поведении пользователей на основе оценки результатов выполнения SQL-запросов // Вестник компьютерных и информационных технологий. – 2013. – №3 – С. 49-54.
  * Григоров А.С. Обнаружение аномалий запросов к базе данных методом определения уровня связанности результата выполнения запроса. Вузовская наука – региону. Материалы 8-й всеросс. научно-техн. конф. – Вологда: ВоГТУ, 2010.
  * Григоров А.С. Метод обнаружения аномалий запросов к базе данных на основе кластеризации результата выполнения запросов. Череповецкие научные чтения – 2010: Материалы Всероссийской научно-практической конференции (3 ноября 2010 г.): В 3ч. Ч. 3: Технические, естественные и экономические науки / Отв. ред. Н.П. Павлова. – Череповец: ЧГУ, 2011. – С. 22-25.
  * Григоров А.С. Обзор методов обнаружения аномалий в SQL-запросах к базам данных [Текст] / А. С. Григоров // Современные тенденции технических наук: материалы междунар. заоч. науч. конф. (г. Уфа, октябрь 2011 г.). / Под общ. ред. Г. Д. Ахметовой.  — Уфа: Лето, 2011. — С. 13-17.
  * Григоров А.С. Использование апостериорного анализа данных для обнаружения аномалий в SQL-запросах к базам данных [Текст] / А. С. Григоров // Молодой ученый. — 2011. — №11. Т.1. — С. 38-40.
  * Григоров А.С. О способе интеграции системы обнаружения аномалий в SQL запросах к базе данных на основе результатов выполнения запроса с приложениями, использующими СУБД в качестве хранилища данных [Текст] / А. С. Григоров // Молодой ученый. — 2011. — №12. Т.1.  — С. 21 24.
  * Григоров А.С. Представление профилей нормального поведения пользователей с помощью масштабируемых фильтров Блума [Текст] / А. С. Григоров // Технические науки: проблемы и перспективы: материалы II междунар. науч. конф. (г. Санкт-Петербург, апрель 2014 г.). — СПб.: Заневская площадь, 2014. — С. 2-5.
