package com.github.meeting.common.cv;

/**
 * 会议
 */
public interface Meeting {


    /**
     * screen share
     */
    public Meeting screenShare () ;




    public void screenRecord () ;

    /***
     * start meeting
     */
    public void start () ;

    /**
     * get current frame data
     * @return
     */
    public byte[] frameData () ;


    public void stop () ;



}
