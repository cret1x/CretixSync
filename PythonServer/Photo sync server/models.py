from database import Base
from sqlalchemy import Table, Column, Integer, ForeignKey, String, DateTime
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base


class Group(Base):
    __tablename__ = 'groups'
    id = Column(Integer, primary_key=True)
    name = Column(String, unique=True)
    password = Column(String)
    path = Column(String)

    devices = relationship("Device", backref='owner')

    def __repr__(self):
        return "<Group('%s')>" % (self.name)



class Device(Base):
    __tablename__ = 'devices'
    id = Column(Integer, primary_key=True)
    login = Column(String, unique=True)
    last_ip = Column(String, unique=True)
    last_status = Column(String)
    group_id = Column(Integer, ForeignKey('groups.id'))

    def __repr__(self):
        return "<Device('%s')>" % (self.login)