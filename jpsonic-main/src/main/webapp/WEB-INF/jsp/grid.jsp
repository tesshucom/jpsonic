<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<script>
    $(function () {
      var $body = $('body');
        $body.append('<button id="showGrid4Debug"/></button>');
        $body.append('<nav id="grid4Debug"></nav>');
      $('#showGrid4Debug').on('click', function () {
        $body.toggleClass('jpsonic');
      });
      $('#grid4Debug').on('click', function () {
        $body.removeClass('jpsonic');
      });
    });
</script>
<style>
    #showGrid4Debug {
      display: block;
      position: absolute;
      top: 10px;
      right: 50%;
      width: 10px;
      height: 10px;
      padding: 10px 10px;
      border-radius: 50%;
      text-decoration: none;
      background: red;
      border-radius: 50%;
      z-index: 2;
      transition: all .2s ease-in;
    }
    #grid4Debug {
      position: fixed;
      z-index:9999;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0,0,0,0.20);
      overflow: scroll;
      z-index: 1;
      transition: all .1s ease-in;
      visibility: hidden;
      opacity: 0;
      background-image:
        repeating-linear-gradient(to right, rgba(255,0,0,0.10),   rgba(255,0,255, .10) 10px, transparent 0px, transparent 30px),
        repeating-linear-gradient(to bottom, rgba(255,0,0,0.10) , rgba(255,0,255, .10) 10px, transparent 0px, transparent 30px);
    }
    .jpsonic #grid4Debug {
      visibility: visible;
      opacity: 1;
    }
</style>
