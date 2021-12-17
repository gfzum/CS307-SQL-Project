SQL建库相关内容：

json文件中，major和department是有ID的，但是传入函数中并没有提到该id。
所以之前建库的时候以为是传入id，但事实上要用自增id。
此外，要使用自增id的还有section_id,semester_id,class_id

之前理解day_of_week是一个集合，因为一周可能有多个时间出现一个class，但是这个数据中day_of_week就只有一个可能。
所以将其由integer[]改成单个。
然后发现week_list同理 所以也改成单个吧。
!!!然后发现只有把这个int数组改为单个才能符合范式的要求，所以就按照int来吧。
然后考虑到这会出现一些一样的classId，于是我们将classes表的主键改成了classId并weekNum
然后就发现了插入数据的时候的问题。于是我们就要分成两块插入，第一块是插入第一条数据获得一个自增id，后面都把classId定义成这个自增id，就可以达成目的了。


优化方法：
或许可以用传入connection的方式做一些优化？



