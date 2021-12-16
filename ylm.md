SQL建库相关内容：

json文件中，major和department是有ID的，但是传入函数中并没有提到该id。
所以之前建库的时候以为是传入id，但事实上要用自增id。
此外，要使用自增id的还有section_id,semester_id,class_id

之前理解day_of_week是一个集合，因为一周可能有多个时间出现一个class，但是这个数据中day_of_week就只有一个可能。
所以将其由integer[]改成单个。


